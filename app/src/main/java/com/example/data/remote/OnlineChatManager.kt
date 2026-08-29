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
        // No-op to remove internet synchronization entirely
        currentPhone = myPhoneNumber
        _isOnlineConnected.value = false
    }

    fun stop() {
        // No-op
        _isOnlineConnected.value = false
    }

    // Stubbed out to remove internet functionality completely as requested
    suspend fun sendOnlineMessage(
        messageId: String,
        senderPhone: String,
        senderName: String,
        recipientPhone: String,
        content: String,
        mediaUrl: String? = null
    ): Boolean {
        messageDao.updateMessageStatus(messageId, MessageStatus.FAILED)
        return false
    }

    fun sendReceipt(targetPhone: String, messageId: String, receiptType: String) {
        // No-op
    }
}
