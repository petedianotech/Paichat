package com.example.data.repository

import android.Manifest
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.example.data.local.dao.ConversationDao
import com.example.data.local.dao.MessageDao
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.MessageStatus
import com.example.data.local.entity.MessageType
import com.example.data.remote.OnlineChatManager
import com.example.data.sync.SmsSyncHelper
import com.example.receiver.SmsStatusReceiver
import com.example.ui.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class MessageRepository(
    private val context: Context,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val contactRepository: ContactRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val smsSyncHelper = SmsSyncHelper(context, conversationDao, messageDao, contactRepository)
    val onlineChatManager = OnlineChatManager(context, conversationDao, messageDao, contactRepository)

    val isOnlineConnected: StateFlow<Boolean> = onlineChatManager.isOnlineConnected

    fun startOnlineSync(myPhoneNumber: String) {
        onlineChatManager.start(myPhoneNumber)
    }

    fun stopOnlineSync() {
        onlineChatManager.stop()
    }

    fun getAllConversations(): Flow<List<ConversationEntity>> =
        conversationDao.getAllConversations()

    fun getConversationById(id: String): Flow<ConversationEntity?> =
        conversationDao.getConversationById(id)

    fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesForConversation(conversationId)

    suspend fun clearUnreadCount(conversationId: String) {
        conversationDao.clearUnreadCount(conversationId)
    }

    suspend fun syncDeviceSms(userPhoneNumber: String): Int {
        return smsSyncHelper.syncDeviceSms(userPhoneNumber)
    }

    suspend fun sendMessage(
        senderPhone: String,
        recipientPhone: String,
        recipientName: String?,
        content: String,
        forcedMessageType: MessageType? = null,
        mediaUrl: String? = null
    ): MessageEntity {
        val conversationId = recipientPhone
        val isInternet = false
        val messageType = MessageType.SMS

        // 1. Ensure conversation exists
        val existingConversation = conversationDao.getConversationByIdDirect(conversationId)
        val timestamp = System.currentTimeMillis()

        val updatedConversation = ConversationEntity(
            conversationId = conversationId,
            phoneNumber = recipientPhone,
            contactName = recipientName ?: existingConversation?.contactName ?: contactRepository.getContactByPhoneNumber(recipientPhone)?.name,
            lastMessage = if (mediaUrl != null) "Photo" else content,
            lastMessageTimestamp = timestamp,
            unreadCount = 0,
            isInternetUser = false
        )
        conversationDao.insertConversation(updatedConversation)

        // 2. Create message
        val messageId = UUID.randomUUID().toString()
        val initialStatus = MessageStatus.SENDING

        val message = MessageEntity(
            messageId = messageId,
            conversationId = conversationId,
            senderPhoneNumber = senderPhone,
            recipientPhoneNumber = recipientPhone,
            content = content,
            timestamp = timestamp,
            messageType = messageType,
            status = initialStatus,
            mediaUrl = mediaUrl
        )
        messageDao.insertMessage(message)

        // 3. Process dispatch based on message type
        if (messageType == MessageType.INTERNET) {
            scope.launch {
                val myName = existingConversation?.contactName ?: "User"
                onlineChatManager.sendOnlineMessage(
                    messageId = messageId,
                    senderPhone = senderPhone,
                    senderName = myName,
                    recipientPhone = recipientPhone,
                    content = content,
                    mediaUrl = mediaUrl
                )
            }
        } else {
            scope.launch {
                sendSmsMessageProcess(messageId, recipientPhone, content)
            }
        }

        return message
    }

    private suspend fun sendSmsMessageProcess(messageId: String, recipientPhone: String, content: String) {
        val hasSendPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasSendPermission) {
            messageDao.updateMessageStatus(messageId, MessageStatus.FAILED)
            return
        }

        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val sentIntent = PendingIntent.getBroadcast(
                context,
                messageId.hashCode(),
                Intent("com.example.SMS_SENT", Uri.parse("sms-sent://$messageId"), context, SmsStatusReceiver::class.java),
                flags
            )

            val deliveryIntent = PendingIntent.getBroadcast(
                context,
                messageId.hashCode(),
                Intent("com.example.SMS_DELIVERED", Uri.parse("sms-delivered://$messageId"), context, SmsStatusReceiver::class.java),
                flags
            )

            val parts = smsManager?.divideMessage(content) ?: ArrayList<String>().apply { add(content) }
            if (parts.size > 1) {
                val sentIntents = ArrayList<PendingIntent>()
                val deliveryIntents = ArrayList<PendingIntent>()
                for (i in parts.indices) {
                    sentIntents.add(sentIntent)
                    deliveryIntents.add(deliveryIntent)
                }
                smsManager?.sendMultipartTextMessage(recipientPhone, null, parts, sentIntents, deliveryIntents)
            } else {
                smsManager?.sendTextMessage(recipientPhone, null, content, sentIntent, deliveryIntent)
            }

            // Save to system sent box if permitted
            try {
                val values = ContentValues().apply {
                    put(Telephony.Sms.ADDRESS, recipientPhone)
                    put(Telephony.Sms.BODY, content)
                    put(Telephony.Sms.DATE, System.currentTimeMillis())
                    put(Telephony.Sms.READ, 1)
                    put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
                }
                context.contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, values)
            } catch (_: Exception) {
                // Safe ignore if permission/provider is locked
            }
        } catch (_: Exception) {
            messageDao.updateMessageStatus(messageId, MessageStatus.FAILED)
        }
    }

    suspend fun fallbackToSendAsSms(messageId: String) {
        val message = messageDao.getMessageById(messageId) ?: return
        val updatedMessage = message.copy(
            messageType = MessageType.SMS,
            status = MessageStatus.SENDING,
            timestamp = System.currentTimeMillis()
        )
        messageDao.updateMessage(updatedMessage)

        scope.launch {
            sendSmsMessageProcess(messageId, message.recipientPhoneNumber, message.content)
        }
    }

    suspend fun receiveIncomingMessage(
        senderPhone: String,
        senderName: String?,
        content: String,
        messageType: MessageType = MessageType.SMS,
        mediaUrl: String? = null
    ) {
        val conversationId = senderPhone
        val timestamp = System.currentTimeMillis()
        val existingConversation = conversationDao.getConversationByIdDirect(conversationId)

        val unread = (existingConversation?.unreadCount ?: 0) + 1
        val isInternet = false
        val resolvedName = senderName ?: existingConversation?.contactName ?: contactRepository.getContactByPhoneNumber(senderPhone)?.name

        val conversation = ConversationEntity(
            conversationId = conversationId,
            phoneNumber = senderPhone,
            contactName = resolvedName,
            lastMessage = if (mediaUrl != null) "Photo" else content,
            lastMessageTimestamp = timestamp,
            unreadCount = unread,
            isInternetUser = false
        )
        conversationDao.insertConversation(conversation)

        val message = MessageEntity(
            messageId = UUID.randomUUID().toString(),
            conversationId = conversationId,
            senderPhoneNumber = senderPhone,
            recipientPhoneNumber = "ME",
            content = content,
            timestamp = timestamp,
            messageType = messageType,
            status = MessageStatus.READ,
            mediaUrl = mediaUrl
        )
        messageDao.insertMessage(message)

        // Trigger system notification for incoming message
        NotificationHelper.showIncomingMessageNotification(
            context = context,
            senderPhone = senderPhone,
            senderName = resolvedName,
            messageText = content
        )
    }

    suspend fun deleteConversation(conversationId: String) {
        messageDao.deleteMessagesForConversation(conversationId)
        conversationDao.deleteConversationById(conversationId)
    }

    suspend fun deleteMessage(messageId: String) {
        messageDao.deleteMessageById(messageId)
    }
}
