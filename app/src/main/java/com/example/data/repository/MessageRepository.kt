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
import com.example.data.local.dao.BlockedContactDao
import com.example.data.local.dao.ConversationDao
import com.example.data.local.dao.MessageDao
import com.example.data.local.dao.QuickResponseDao
import com.example.data.local.dao.ScheduledMessageDao
import com.example.data.local.entity.BlockedContactEntity
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.MessageStatus
import com.example.data.local.entity.MessageType
import com.example.data.local.entity.QuickResponseEntity
import com.example.data.local.entity.ScheduledMessageEntity
import com.example.data.sync.SmsSyncHelper
import com.example.receiver.SmsStatusReceiver
import com.example.ui.util.NotificationHelper
import com.example.ui.util.PhoneNumberUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID

class MessageRepository(
    private val context: Context,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val scheduledMessageDao: ScheduledMessageDao,
    private val blockedContactDao: BlockedContactDao,
    private val quickResponseDao: QuickResponseDao,
    private val contactRepository: ContactRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val smsSyncHelper = SmsSyncHelper(context, conversationDao, messageDao, contactRepository)

    init {
        scope.launch {
            seedDefaultQuickResponsesIfNeeded()
        }
    }

    private suspend fun seedDefaultQuickResponsesIfNeeded() {
        if (quickResponseDao.getCount() == 0) {
            val defaults = listOf(
                QuickResponseEntity(text = "I'm on my way!", isDefault = true, orderIndex = 0),
                QuickResponseEntity(text = "Can't talk right now, text you later.", isDefault = true, orderIndex = 1),
                QuickResponseEntity(text = "Sounds good, let's do it!", isDefault = true, orderIndex = 2),
                QuickResponseEntity(text = "Please call me when you get a chance.", isDefault = true, orderIndex = 3),
                QuickResponseEntity(text = "Running about 5 minutes late!", isDefault = true, orderIndex = 4),
                QuickResponseEntity(text = "Thanks! Talk soon.", isDefault = true, orderIndex = 5)
            )
            quickResponseDao.insertQuickResponses(defaults)
        }
    }

    fun getAllConversations(): Flow<List<ConversationEntity>> =
        conversationDao.getAllConversations()

    fun getBlockedConversations(): Flow<List<ConversationEntity>> =
        conversationDao.getBlockedConversations()

    fun getConversationById(id: String): Flow<ConversationEntity?> =
        conversationDao.getConversationById(id)

    fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesForConversationFlexible(
            conversationId = conversationId,
            normalizedId = PhoneNumberUtil.normalize(conversationId)
        )

    fun getScheduledMessagesForConversation(conversationId: String): Flow<List<ScheduledMessageEntity>> =
        scheduledMessageDao.getPendingForConversation(conversationId)

    fun getAllScheduledMessages(): Flow<List<ScheduledMessageEntity>> =
        scheduledMessageDao.getAllPendingScheduledMessages()

    fun getAllBlockedContacts(): Flow<List<BlockedContactEntity>> =
        blockedContactDao.getAllBlocked()

    fun getAllQuickResponses(): Flow<List<QuickResponseEntity>> =
        quickResponseDao.getAllQuickResponses()

    suspend fun addQuickResponse(text: String) {
        quickResponseDao.insertQuickResponse(
            QuickResponseEntity(text = text.trim(), isDefault = false)
        )
    }

    suspend fun deleteQuickResponse(id: Long) {
        quickResponseDao.deleteQuickResponse(id)
    }

    suspend fun clearUnreadCount(conversationId: String) {
        conversationDao.clearUnreadCount(conversationId)
    }

    suspend fun markAsUnread(conversationId: String) {
        conversationDao.markAsUnread(conversationId)
    }

    suspend fun setPinned(conversationId: String, isPinned: Boolean) {
        conversationDao.setPinned(conversationId, isPinned)
    }

    suspend fun setConversationCustomColor(conversationId: String, colorHex: String?) {
        conversationDao.setCustomColor(conversationId, colorHex)
    }

    suspend fun blockContact(phoneNumber: String, contactName: String?, reason: String? = null) {
        blockedContactDao.block(
            BlockedContactEntity(
                phoneNumber = phoneNumber,
                contactName = contactName,
                blockedTimestamp = System.currentTimeMillis(),
                reason = reason
            )
        )
        conversationDao.setBlocked(phoneNumber, true)
    }

    suspend fun unblockContact(phoneNumber: String) {
        blockedContactDao.unblock(phoneNumber)
        conversationDao.setBlocked(phoneNumber, false)
    }

    suspend fun isContactBlocked(phoneNumber: String): Boolean {
        return blockedContactDao.isBlocked(phoneNumber)
    }

    suspend fun scheduleMessage(
        recipientPhone: String,
        recipientName: String?,
        content: String,
        scheduledTimestamp: Long
    ): Long {
        val conversationId = recipientPhone
        val entity = ScheduledMessageEntity(
            conversationId = conversationId,
            recipientPhoneNumber = recipientPhone,
            recipientName = recipientName,
            content = content,
            scheduledTimestamp = scheduledTimestamp,
            isSent = false
        )
        return scheduledMessageDao.insert(entity)
    }

    suspend fun cancelScheduledMessage(id: Long) {
        scheduledMessageDao.deleteById(id)
    }

    suspend fun sendScheduledMessageNow(id: Long, recipientPhone: String, recipientName: String?, content: String) {
        scheduledMessageDao.deleteById(id)
        sendMessage(recipientPhone, recipientName, content)
    }

    suspend fun checkAndDispatchDueScheduledMessages() {
        val now = System.currentTimeMillis()
        val dueMessages = scheduledMessageDao.getDueScheduledMessages(now)
        for (scheduled in dueMessages) {
            scheduledMessageDao.markAsSent(scheduled.id)
            sendMessage(
                recipientPhone = scheduled.recipientPhoneNumber,
                recipientName = scheduled.recipientName,
                content = scheduled.content
            )
        }
    }

    suspend fun syncDeviceSms(): Int {
        return smsSyncHelper.syncDeviceSms()
    }

    suspend fun sendMessage(
        recipientPhone: String,
        recipientName: String?,
        content: String,
        mediaUrl: String? = null,
        subscriptionId: Int? = null
    ): MessageEntity {
        val conversationId = recipientPhone
        val messageType = if (mediaUrl != null) MessageType.MMS else MessageType.SMS
        val messageSummary = when {
            mediaUrl != null && (mediaUrl.endsWith(".m4a") || mediaUrl.contains("voice_")) -> "🎵 Voice message"
            mediaUrl != null -> "📷 Photo attachment"
            else -> content
        }

        // 1. Ensure conversation exists
        val existingConversation = conversationDao.getConversationByIdDirect(conversationId)
        val timestamp = System.currentTimeMillis()

        val updatedConversation = ConversationEntity(
            conversationId = conversationId,
            phoneNumber = recipientPhone,
            contactName = recipientName ?: existingConversation?.contactName ?: contactRepository.getContactByPhoneNumber(recipientPhone)?.name,
            lastMessage = messageSummary,
            lastMessageTimestamp = timestamp,
            unreadCount = 0,
            isInternetUser = false,
            isPinned = existingConversation?.isPinned ?: false,
            isBlocked = existingConversation?.isBlocked ?: false,
            customColorHex = existingConversation?.customColorHex
        )
        conversationDao.insertConversation(updatedConversation)

        // 2. Create message (sender is always "ME" for outgoing)
        val messageId = UUID.randomUUID().toString()
        val initialStatus = MessageStatus.SENDING

        val message = MessageEntity(
            messageId = messageId,
            conversationId = conversationId,
            senderPhoneNumber = "ME",
            recipientPhoneNumber = recipientPhone,
            content = content,
            timestamp = timestamp,
            messageType = messageType,
            status = initialStatus,
            mediaUrl = mediaUrl
        )
        messageDao.insertMessage(message)

        // 3. Dispatch real SMS via SIM card
        scope.launch {
            sendSmsMessageProcess(messageId, recipientPhone, content, subscriptionId)
        }

        return message
    }

    suspend fun sendBroadcastOrGroupSms(
        recipients: List<String>,
        content: String,
        subscriptionId: Int? = null
    ) {
        for (phone in recipients) {
            val contact = contactRepository.getContactByPhoneNumber(phone)
            sendMessage(
                recipientPhone = phone,
                recipientName = contact?.name,
                content = content,
                subscriptionId = subscriptionId
            )
        }
    }

    suspend fun retryMessage(messageId: String) {
        val message = messageDao.getMessageById(messageId) ?: return
        val updated = message.copy(
            status = MessageStatus.SENDING,
            timestamp = System.currentTimeMillis()
        )
        messageDao.updateMessage(updated)

        scope.launch {
            sendSmsMessageProcess(messageId, message.recipientPhoneNumber, message.content, null)
        }
    }

    private suspend fun sendSmsMessageProcess(
        messageId: String,
        recipientPhone: String,
        content: String,
        subscriptionId: Int?
    ) {
        val hasSendPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasSendPermission) {
            messageDao.updateMessageStatus(messageId, MessageStatus.FAILED)
            return
        }

        try {
            val smsManager: SmsManager? = try {
                if (subscriptionId != null && subscriptionId >= 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        context.getSystemService(SmsManager::class.java)?.createForSubscriptionId(subscriptionId)
                    } else {
                        @Suppress("DEPRECATION")
                        SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java) ?: @Suppress("DEPRECATION") SmsManager.getDefault()
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    context.getSystemService(SmsManager::class.java) ?: @Suppress("DEPRECATION") SmsManager.getDefault()
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
            } catch (_: Exception) {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            if (smsManager == null) {
                messageDao.updateMessageStatus(messageId, MessageStatus.FAILED)
                return
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

            val parts = smsManager.divideMessage(content) ?: ArrayList<String>().apply { add(content) }
            if (parts.size > 1) {
                val sentIntents = ArrayList<PendingIntent>()
                val deliveryIntents = ArrayList<PendingIntent>()
                for (i in parts.indices) {
                    sentIntents.add(sentIntent)
                    deliveryIntents.add(deliveryIntent)
                }
                smsManager.sendMultipartTextMessage(recipientPhone, null, parts, sentIntents, deliveryIntents)
            } else {
                smsManager.sendTextMessage(recipientPhone, null, content, sentIntent, deliveryIntent)
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

    suspend fun receiveIncomingMessage(
        senderPhone: String,
        senderName: String?,
        content: String,
        messageType: MessageType = MessageType.SMS,
        mediaUrl: String? = null
    ) {
        // Drop message if blocked
        if (blockedContactDao.isBlocked(senderPhone)) {
            return
        }

        val conversationId = senderPhone
        val timestamp = System.currentTimeMillis()
        val existingConversation = conversationDao.getConversationByIdDirect(conversationId)

        val unread = (existingConversation?.unreadCount ?: 0) + 1
        val resolvedName = senderName ?: existingConversation?.contactName ?: contactRepository.getContactByPhoneNumber(senderPhone)?.name

        val messageSummary = when {
            mediaUrl != null && (mediaUrl.endsWith(".m4a") || mediaUrl.contains("voice_")) -> "🎵 Voice message"
            mediaUrl != null -> "📷 Photo attachment"
            messageType == MessageType.MMS -> "📎 MMS message"
            else -> content
        }

        val conversation = ConversationEntity(
            conversationId = conversationId,
            phoneNumber = senderPhone,
            contactName = resolvedName,
            lastMessage = messageSummary,
            lastMessageTimestamp = timestamp,
            unreadCount = unread,
            isInternetUser = false,
            isPinned = existingConversation?.isPinned ?: false,
            isBlocked = false,
            customColorHex = existingConversation?.customColorHex
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
            status = MessageStatus.DELIVERED,
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
