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
import java.util.UUID

class SmsSyncHelper(
    private val context: Context,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val contactRepository: ContactRepository
) {
    suspend fun syncDeviceSms(userPhoneNumber: String): Int = withContext(Dispatchers.IO) {
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

            val cursor = context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC LIMIT 100"
            )

            cursor?.use { c ->
                val addressIdx = c.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = c.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = c.getColumnIndex(Telephony.Sms.DATE)
                val typeIdx = c.getColumnIndex(Telephony.Sms.TYPE)

                while (c.moveToNext()) {
                    val address = if (addressIdx >= 0) c.getString(addressIdx) else null
                    val body = if (bodyIdx >= 0) c.getString(bodyIdx) else ""
                    val date = if (dateIdx >= 0) c.getLong(dateIdx) else System.currentTimeMillis()
                    val type = if (typeIdx >= 0) c.getInt(typeIdx) else Telephony.Sms.MESSAGE_TYPE_INBOX

                    if (!address.isNullOrBlank() && body.isNotBlank()) {
                        val conversationId = address
                        val isFromMe = type == Telephony.Sms.MESSAGE_TYPE_SENT || type == Telephony.Sms.MESSAGE_TYPE_OUTBOX
                        val sender = if (isFromMe) userPhoneNumber else address
                        val recipient = if (isFromMe) address else userPhoneNumber

                        // Ensure conversation exists
                        val existingConv = conversationDao.getConversationByIdDirect(conversationId)
                        val contact = contactRepository.getContactByPhoneNumber(address)
                        val contactName = existingConv?.contactName ?: contact?.name

                        val conv = ConversationEntity(
                            conversationId = conversationId,
                            phoneNumber = address,
                            contactName = contactName,
                            lastMessage = body,
                            lastMessageTimestamp = date,
                            unreadCount = 0,
                            isInternetUser = contact?.isInternetUser ?: false
                        )
                        conversationDao.insertConversation(conv)

                        // Insert message
                        val msg = MessageEntity(
                            messageId = "sms_sys_${UUID.randomUUID()}",
                            conversationId = conversationId,
                            senderPhoneNumber = sender,
                            recipientPhoneNumber = recipient,
                            content = body,
                            timestamp = date,
                            messageType = MessageType.SMS,
                            status = if (isFromMe) MessageStatus.SENT else MessageStatus.READ
                        )
                        messageDao.insertMessage(msg)
                        importedCount++
                    }
                }
            }
        } catch (_: Exception) {
            // Handled safely
        }
        importedCount
    }
}
