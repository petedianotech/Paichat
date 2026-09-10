package com.example.data.sync

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.example.data.local.dao.ConversationDao
import com.example.data.local.dao.MessageDao
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.MessageStatus
import com.example.data.local.entity.MessageType
import com.example.data.repository.ContactRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmsSyncHelper(
    private val context: Context,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val contactRepository: ContactRepository
) {
    suspend fun syncDeviceSms(): Int = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return@withContext 0
        }

        var importedCount = 0
        try {
            val projection = arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE,
                Telephony.Sms.READ
            )

            // Query all SMS messages, sorted descending by date
            val cursor = context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )

            val idIdx = cursor?.getColumnIndex(Telephony.Sms._ID) ?: -1
            val addressIdx = cursor?.getColumnIndex(Telephony.Sms.ADDRESS) ?: -1
            val bodyIdx = cursor?.getColumnIndex(Telephony.Sms.BODY) ?: -1
            val dateIdx = cursor?.getColumnIndex(Telephony.Sms.DATE) ?: -1
            val typeIdx = cursor?.getColumnIndex(Telephony.Sms.TYPE) ?: -1
            val readIdx = cursor?.getColumnIndex(Telephony.Sms.READ) ?: -1

            val messagesToInsert = ArrayList<MessageEntity>()
            
            // To group by conversations and determine the latest message for each
            val conversationLatestMap = HashMap<String, Pair<String, Long>>() // address -> Pair(body, date)
            val conversationUnreadMap = HashMap<String, Int>() // address -> unreadCount
            val contactNameMap = HashMap<String, String?>()

            // Pre-load all existing conversations to avoid overwriting properties like isPinned/isBlocked or custom color
            val existingConversations = conversationDao.getAllConversationsDirect().associateBy { it.conversationId }

            cursor?.use { c ->
                while (c.moveToNext()) {
                    val smsId = if (idIdx >= 0) c.getLong(idIdx) else continue
                    val address = if (addressIdx >= 0) c.getString(addressIdx) else null
                    val body = if (bodyIdx >= 0) c.getString(bodyIdx) else ""
                    val date = if (dateIdx >= 0) c.getLong(dateIdx) else System.currentTimeMillis()
                    val type = if (typeIdx >= 0) c.getInt(typeIdx) else Telephony.Sms.MESSAGE_TYPE_INBOX
                    val read = if (readIdx >= 0) c.getInt(readIdx) else 1

                    if (!address.isNullOrBlank()) {
                        val conversationId = address
                        val isFromMe = type == Telephony.Sms.MESSAGE_TYPE_SENT || type == Telephony.Sms.MESSAGE_TYPE_OUTBOX
                        val sender = if (isFromMe) "ME" else address
                        val recipient = if (isFromMe) address else "ME"

                        // Track latest message for the conversation
                        val currentLatest = conversationLatestMap[address]
                        if (currentLatest == null || date > currentLatest.second) {
                            conversationLatestMap[address] = Pair(body, date)
                        }

                        // Track unread count (if incoming and read == 0)
                        if (!isFromMe && read == 0) {
                            conversationUnreadMap[address] = (conversationUnreadMap[address] ?: 0) + 1
                        }

                        // Add message entity
                        val msg = MessageEntity(
                            messageId = "sms_${smsId}",
                            conversationId = conversationId,
                            senderPhoneNumber = sender,
                            recipientPhoneNumber = recipient,
                            content = body,
                            timestamp = date,
                            messageType = MessageType.SMS,
                            status = if (isFromMe) MessageStatus.SENT else MessageStatus.READ
                        )
                        messagesToInsert.add(msg)
                    }
                }
            }

            // Build Conversation entities
            val conversationsToInsert = ArrayList<ConversationEntity>()
            for ((address, latestInfo) in conversationLatestMap) {
                val existingConv = existingConversations[address]
                
                // Get contact name (cache name per address)
                val contactName = existingConv?.contactName ?: contactNameMap.getOrPut(address) {
                    contactRepository.getContactByPhoneNumber(address)?.name
                }

                val unreadCount = conversationUnreadMap[address] ?: 0

                val conv = ConversationEntity(
                    conversationId = address,
                    phoneNumber = address,
                    contactName = contactName,
                    lastMessage = latestInfo.first,
                    lastMessageTimestamp = latestInfo.second,
                    unreadCount = if (existingConv != null) existingConv.unreadCount + unreadCount else unreadCount,
                    isInternetUser = existingConv?.isInternetUser ?: false,
                    isPinned = existingConv?.isPinned ?: false,
                    isBlocked = existingConv?.isBlocked ?: false,
                    customColorHex = existingConv?.customColorHex
                )
                conversationsToInsert.add(conv)
            }

            // Perform batch inserts
            if (conversationsToInsert.isNotEmpty()) {
                conversationDao.insertConversations(conversationsToInsert)
            }

            // Insert messages in chunks of 500 to avoid Room/SQLite binder limits and keep memory usage bounded
            val chunkSize = 500
            for (i in messagesToInsert.indices step chunkSize) {
                val end = minOf(i + chunkSize, messagesToInsert.size)
                val chunk = messagesToInsert.subList(i, end)
                messageDao.insertMessagesIgnore(chunk)
                importedCount += chunk.size
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        importedCount
    }
}
