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
import com.example.data.model.SyncProgress
import com.example.data.sync.SmsSyncHelper
import com.example.receiver.SmsStatusReceiver
import com.example.ui.util.NotificationHelper
import com.example.ui.util.PhoneNumberUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

class MessageRepository(
    private val context: Context,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val scheduledMessageDao: ScheduledMessageDao,
    private val blockedContactDao: BlockedContactDao,
    private val quickResponseDao: QuickResponseDao,
    private val contactRepository: ContactRepository,
    private val userPreferences: com.example.data.preference.UserPreferences
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val smsSyncHelper = SmsSyncHelper(context, conversationDao, messageDao, contactRepository, userPreferences)

    val syncProgress: StateFlow<SyncProgress> = smsSyncHelper.syncProgress

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

    fun getMessagesForConversationPaged(conversationId: String, limit: Int): Flow<List<MessageEntity>> =
        messageDao.getMessagesForConversationFlexiblePaged(
            conversationId = conversationId,
            normalizedId = PhoneNumberUtil.normalize(conversationId),
            limit = limit
        ).map { list -> list.reversed() }

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

    suspend fun setConversationCustomWallpaper(conversationId: String, wallpaper: String?) {
        conversationDao.setCustomWallpaper(conversationId, wallpaper)
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
            customColorHex = existingConversation?.customColorHex,
            customWallpaper = existingConversation?.customWallpaper
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

        // 4. Start 60-second timeout watcher for quick reply & standard SMS
        startMessageTimeoutWatcher(messageId, recipientPhone, recipientName, content)

        return message
    }

    private fun startMessageTimeoutWatcher(
        messageId: String,
        recipientPhone: String,
        recipientName: String?,
        content: String
    ) {
        scope.launch {
            kotlinx.coroutines.delay(60_000L) // 60 seconds threshold
            val currentMsg = messageDao.getMessageById(messageId)
            if (currentMsg != null && (currentMsg.status == MessageStatus.SENDING || currentMsg.status == MessageStatus.FAILED)) {
                val userPrefs = com.example.data.preference.UserPreferences(context)
                val appSettings = userPrefs.appSettings.value

                if (appSettings.autoRetryAfterTimeout) {
                    messageDao.updateMessageStatus(messageId, MessageStatus.SENDING)
                    sendSmsMessageProcess(messageId, recipientPhone, content, null)

                    NotificationHelper.showSendingTimeoutNotification(
                        context = context,
                        recipientPhone = recipientPhone,
                        recipientName = recipientName,
                        messageId = messageId,
                        content = content,
                        isAutoRetrying = true
                    )
                } else {
                    messageDao.updateMessageStatus(messageId, MessageStatus.FAILED)
                    NotificationHelper.showSendingTimeoutNotification(
                        context = context,
                        recipientPhone = recipientPhone,
                        recipientName = recipientName,
                        messageId = messageId,
                        content = content,
                        isAutoRetrying = false
                    )
                }
            }
        }
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

        startMessageTimeoutWatcher(messageId, message.recipientPhoneNumber, null, message.content)
    }

    suspend fun cancelRetry(messageId: String) {
        val message = messageDao.getMessageById(messageId) ?: return
        val updated = message.copy(
            status = MessageStatus.CANCELLED
        )
        messageDao.updateMessage(updated)
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
                Intent("com.example.SMS_SENT", Uri.parse("sms-sent://$messageId"), context, SmsStatusReceiver::class.java).apply {
                    putExtra("extra_message_id", messageId)
                },
                flags
            )

            val deliveryIntent = PendingIntent.getBroadcast(
                context,
                messageId.hashCode() + 1,
                Intent("com.example.SMS_DELIVERED", Uri.parse("sms-delivered://$messageId"), context, SmsStatusReceiver::class.java).apply {
                    putExtra("extra_message_id", messageId)
                },
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
        val normalizedPhone = PhoneNumberUtil.normalize(senderPhone)
        // Drop message if blocked
        if (blockedContactDao.isBlocked(normalizedPhone) || blockedContactDao.isBlocked(senderPhone)) {
            return
        }

        val conversationId = normalizedPhone
        val timestamp = System.currentTimeMillis()

        // 1. Database-level deduplication check
        val recentDuplicate = messageDao.findRecentIncomingMessage(normalizedPhone, content, timestamp - 10_000L)
            ?: messageDao.findRecentIncomingMessage(senderPhone, content, timestamp - 10_000L)
        if (recentDuplicate != null) {
            android.util.Log.d("MessageRepository", "Duplicate message from $senderPhone already stored in DB - ignoring")
            return
        }

        val existingConversation = conversationDao.getConversationByIdDirect(conversationId)
        val unread = (existingConversation?.unreadCount ?: 0) + 1
        val resolvedName = senderName ?: existingConversation?.contactName ?: contactRepository.getContactByPhoneNumber(normalizedPhone)?.name ?: contactRepository.getContactByPhoneNumber(senderPhone)?.name

        val messageSummary = when {
            mediaUrl != null && (mediaUrl.endsWith(".m4a") || mediaUrl.contains("voice_")) -> "🎵 Voice message"
            mediaUrl != null -> "📷 Photo attachment"
            messageType == MessageType.MMS -> "📎 MMS message"
            else -> content
        }

        val conversation = ConversationEntity(
            conversationId = conversationId,
            phoneNumber = normalizedPhone,
            contactName = resolvedName,
            lastMessage = messageSummary,
            lastMessageTimestamp = timestamp,
            unreadCount = unread,
            isInternetUser = false,
            isPinned = existingConversation?.isPinned ?: false,
            isBlocked = false,
            customColorHex = existingConversation?.customColorHex,
            customWallpaper = existingConversation?.customWallpaper
        )
        conversationDao.insertConversation(conversation)

        val message = MessageEntity(
            messageId = UUID.randomUUID().toString(),
            conversationId = conversationId,
            senderPhoneNumber = normalizedPhone,
            recipientPhoneNumber = "ME",
            content = content,
            timestamp = timestamp,
            messageType = messageType,
            status = MessageStatus.DELIVERED,
            mediaUrl = mediaUrl
        )
        messageDao.insertMessage(message)

        val appSettings = userPreferences.appSettings.value

        // 2. Trigger standard Android notification (always reliable in notification shade)
        NotificationHelper.showIncomingMessageNotification(
            context = context,
            senderPhone = normalizedPhone,
            senderName = resolvedName,
            messageText = content,
            vibratePattern = appSettings.notificationVibratePattern
        )

        // 3. Trigger custom preview popup if enabled in settings and user is not already active in this chat
        if (appSettings.quickReplyPopup && com.example.ui.util.ActiveConversationTracker.activeConversationId != normalizedPhone) {
            NotificationHelper.launchQuickReplyPopup(
                context = context,
                senderPhone = normalizedPhone,
                senderName = resolvedName,
                messageText = content
            )
        }
    }

    fun searchMessages(query: String): Flow<List<MessageEntity>> {
        return messageDao.searchMessages(query)
    }

    suspend fun getConversationDirect(conversationId: String): ConversationEntity? {
        return conversationDao.getConversationByIdDirect(conversationId)
    }

    suspend fun getMessagesDirect(conversationId: String): List<MessageEntity> {
        return messageDao.getMessagesForConversationFlexibleDirect(
            conversationId = conversationId,
            normalizedId = PhoneNumberUtil.normalize(conversationId)
        )
    }

    suspend fun restoreConversationAndMessages(conversation: ConversationEntity, messages: List<MessageEntity>) {
        conversationDao.insertConversation(conversation)
        if (messages.isNotEmpty()) {
            messageDao.insertMessages(messages)
        }
    }

    suspend fun restoreMessages(messages: List<MessageEntity>) {
        if (messages.isNotEmpty()) {
            messageDao.insertMessages(messages)
        }
    }

    suspend fun deleteConversation(conversationId: String) {
        messageDao.deleteMessagesForConversation(conversationId)
        conversationDao.deleteConversationById(conversationId)
    }

    suspend fun deleteMessage(messageId: String) {
        messageDao.deleteMessageById(messageId)
    }

    suspend fun deleteMessages(messageIds: Set<String>) {
        messageIds.forEach { messageDao.deleteMessageById(it) }
    }
}
