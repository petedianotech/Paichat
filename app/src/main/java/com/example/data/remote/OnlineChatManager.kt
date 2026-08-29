package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.local.dao.ConversationDao
import com.example.data.local.dao.MessageDao
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.MessageStatus
import com.example.data.local.entity.MessageType
import com.example.data.repository.ContactRepository
import com.example.ui.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class OnlineMessagePayload(
    val type: String, // CHAT_MESSAGE, DELIVERY_RECEIPT, READ_RECEIPT
    val messageId: String,
    val senderPhone: String,
    val senderName: String,
    val recipientPhone: String,
    val content: String,
    val timestamp: Long,
    val mediaUrl: String? = null
)

class OnlineChatManager(
    private val context: Context,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val contactRepository: ContactRepository
) {
    private val TAG = "OnlineChatManager"
    private val scope = CoroutineScope(Dispatchers.IO)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Indefinite read timeout for WebSockets
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val processedMessageIds = ConcurrentHashMap.newKeySet<String>()

    private var activeWebSocket: WebSocket? = null
    private var listeningJob: Job? = null
    private var currentPhone: String = ""

    private val _isOnlineConnected = MutableStateFlow(false)
    val isOnlineConnected: StateFlow<Boolean> = _isOnlineConnected.asStateFlow()

    companion object {
        private const val BASE_URL = "https://ntfy.sh"
        private const val BASE_WS_URL = "wss://ntfy.sh"
        private const val TOPIC_PREFIX = "paichat_inbox_"

        fun getCleanTopic(phoneNumber: String): String {
            val digits = phoneNumber.filter { it.isDigit() }
            val clean = if (digits.isBlank()) "guest_user" else digits
            return "${TOPIC_PREFIX}$clean"
        }
    }

    fun start(myPhoneNumber: String) {
        if (myPhoneNumber.isBlank()) return
        if (myPhoneNumber == currentPhone && _isOnlineConnected.value) return

        currentPhone = myPhoneNumber
        stop()

        listeningJob = scope.launch {
            // 1. Initial catch-up for offline messages
            fetchMissedMessages(myPhoneNumber)

            // 2. Continuous real-time listener loop with auto-reconnect
            while (isActive) {
                try {
                    connectWebSocket(myPhoneNumber)
                } catch (e: Exception) {
                    Log.e(TAG, "WebSocket error: ${e.message}")
                    _isOnlineConnected.value = false
                }
                delay(5000)
            }
        }
    }

    fun stop() {
        listeningJob?.cancel()
        activeWebSocket?.close(1000, "App paused")
        activeWebSocket = null
        _isOnlineConnected.value = false
    }

    private suspend fun connectWebSocket(myPhoneNumber: String) {
        val topic = getCleanTopic(myPhoneNumber)
        val wsUrl = "$BASE_WS_URL/$topic/ws"
        val request = Request.Builder().url(wsUrl).build()

        activeWebSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Connected to online chat stream: $topic")
                _isOnlineConnected.value = true
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                scope.launch {
                    handleIncomingRawPayload(text)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                _isOnlineConnected.value = false
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "WebSocket disconnected: ${t.message}")
                _isOnlineConnected.value = false
            }
        })
    }

    private suspend fun fetchMissedMessages(myPhoneNumber: String) {
        withContext(Dispatchers.IO) {
            try {
                val topic = getCleanTopic(myPhoneNumber)
                val url = "$BASE_URL/$topic/json?since=12h"
                val request = Request.Builder().url(url).build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful && response.body != null) {
                        val reader = BufferedReader(InputStreamReader(response.body!!.byteStream()))
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            line?.let { raw ->
                                if (raw.isNotBlank()) {
                                    handleIncomingRawPayload(raw)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch missed messages: ${e.message}")
            }
        }
    }

    private suspend fun handleIncomingRawPayload(rawJson: String) {
        try {
            val root = JSONObject(rawJson)
            // ntfy wraps the payload in "message" field if published as text/json
            val rawMessage = if (root.has("message")) root.getString("message") else rawJson
            if (rawMessage.isBlank()) return

            val payloadJson = try {
                JSONObject(rawMessage)
            } catch (_: Exception) {
                return
            }

            val type = payloadJson.optString("type", "CHAT_MESSAGE")
            val messageId = payloadJson.optString("messageId", "")
            val senderPhone = payloadJson.optString("senderPhone", "")
            val senderName = payloadJson.optString("senderName", "")
            val recipientPhone = payloadJson.optString("recipientPhone", "")
            val content = payloadJson.optString("content", "")
            val timestamp = payloadJson.optLong("timestamp", System.currentTimeMillis())
            val mediaUrl = if (payloadJson.has("mediaUrl") && !payloadJson.isNull("mediaUrl")) payloadJson.optString("mediaUrl") else null

            if (messageId.isBlank() || senderPhone.isBlank()) return

            // Prevent processing own sent messages echoed back
            val cleanSender = senderPhone.filter { it.isDigit() }
            val cleanCurrent = currentPhone.filter { it.isDigit() }
            if (cleanSender == cleanCurrent && cleanSender.isNotBlank()) return

            when (type) {
                "CHAT_MESSAGE" -> {
                    if (processedMessageIds.add(messageId)) {
                        // Check if already in DB
                        val existing = messageDao.getMessageById(messageId)
                        if (existing == null) {
                            handleChatMessage(
                                messageId = messageId,
                                senderPhone = senderPhone,
                                senderName = senderName,
                                content = content,
                                timestamp = timestamp,
                                mediaUrl = mediaUrl
                            )
                            // Send delivery receipt back to sender
                            sendReceipt(senderPhone, messageId, "DELIVERY_RECEIPT")
                        }
                    }
                }
                "DELIVERY_RECEIPT" -> {
                    messageDao.updateMessageStatus(messageId, MessageStatus.DELIVERED)
                }
                "READ_RECEIPT" -> {
                    messageDao.updateMessageStatus(messageId, MessageStatus.READ)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing payload: ${e.message}")
        }
    }

    private suspend fun handleChatMessage(
        messageId: String,
        senderPhone: String,
        senderName: String,
        content: String,
        timestamp: Long,
        mediaUrl: String?
    ) {
        val conversationId = senderPhone
        val existingConversation = conversationDao.getConversationByIdDirect(conversationId)
        val unread = (existingConversation?.unreadCount ?: 0) + 1
        val resolvedName = if (senderName.isNotBlank()) senderName else existingConversation?.contactName ?: contactRepository.getContactByPhoneNumber(senderPhone)?.name

        val conversation = ConversationEntity(
            conversationId = conversationId,
            phoneNumber = senderPhone,
            contactName = resolvedName,
            lastMessage = if (mediaUrl != null) "Photo" else content,
            lastMessageTimestamp = timestamp,
            unreadCount = unread,
            isInternetUser = true
        )
        conversationDao.insertConversation(conversation)

        val message = MessageEntity(
            messageId = messageId,
            conversationId = conversationId,
            senderPhoneNumber = senderPhone,
            recipientPhoneNumber = currentPhone,
            content = content,
            timestamp = timestamp,
            messageType = MessageType.INTERNET,
            status = MessageStatus.DELIVERED,
            mediaUrl = mediaUrl
        )
        messageDao.insertMessage(message)

        // Show status bar notification
        NotificationHelper.showIncomingMessageNotification(
            context = context,
            senderPhone = senderPhone,
            senderName = resolvedName,
            messageText = content
        )
    }

    suspend fun sendOnlineMessage(
        messageId: String,
        senderPhone: String,
        senderName: String,
        recipientPhone: String,
        content: String,
        mediaUrl: String? = null
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val topic = getCleanTopic(recipientPhone)
                val url = "$BASE_URL/$topic"

                val json = JSONObject().apply {
                    put("type", "CHAT_MESSAGE")
                    put("messageId", messageId)
                    put("senderPhone", senderPhone)
                    put("senderName", senderName)
                    put("recipientPhone", recipientPhone)
                    put("content", content)
                    put("timestamp", System.currentTimeMillis())
                    if (mediaUrl != null) put("mediaUrl", mediaUrl)
                }

                val body = json.toString().toRequestBody("text/plain; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Title", "PaiChat Message from $senderName")
                    .addHeader("Priority", "high")
                    .build()

                val response = client.newCall(request).execute()
                val success = response.isSuccessful
                response.close()

                if (success) {
                    messageDao.updateMessageStatus(messageId, MessageStatus.SENT)
                } else {
                    messageDao.updateMessageStatus(messageId, MessageStatus.FAILED)
                }
                success
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send online message: ${e.message}")
                messageDao.updateMessageStatus(messageId, MessageStatus.FAILED)
                false
            }
        }
    }

    fun sendReceipt(targetPhone: String, messageId: String, receiptType: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val topic = getCleanTopic(targetPhone)
                val url = "$BASE_URL/$topic"

                val json = JSONObject().apply {
                    put("type", receiptType)
                    put("messageId", messageId)
                    put("senderPhone", currentPhone)
                    put("recipientPhone", targetPhone)
                    put("timestamp", System.currentTimeMillis())
                }

                val body = json.toString().toRequestBody("text/plain; charset=utf-8".toMediaType())
                val request = Request.Builder().url(url).post(body).build()
                client.newCall(request).execute().close()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send receipt: ${e.message}")
            }
        }
    }
}
