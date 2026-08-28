package com.example.receiver

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.data.local.database.PulseChatDatabase
import com.example.data.local.entity.MessageType
import com.example.data.repository.ContactRepository
import com.example.data.repository.MessageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION ||
            action == Telephony.Sms.Intents.SMS_DELIVER_ACTION ||
            action == "android.provider.Telephony.SMS_DELIVERED"
        ) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (!messages.isNullOrEmpty()) {
                val db = PulseChatDatabase.getDatabase(context)
                val contactRepo = ContactRepository()
                val messageRepo = MessageRepository(context, db.conversationDao(), db.messageDao(), contactRepo)

                val scope = CoroutineScope(Dispatchers.IO)
                scope.launch {
                    // Group multipart messages by sender
                    val senderToBodyMap = mutableMapOf<String, StringBuilder>()
                    for (sms in messages) {
                        val sender = sms.originatingAddress ?: continue
                        val body = sms.messageBody ?: ""
                        val sb = senderToBodyMap.getOrPut(sender) { StringBuilder() }
                        sb.append(body)
                    }

                    for ((sender, bodyBuilder) in senderToBodyMap) {
                        val fullBody = bodyBuilder.toString()
                        messageRepo.receiveIncomingMessage(
                            senderPhone = sender,
                            senderName = null,
                            content = fullBody,
                            messageType = MessageType.SMS
                        )

                        // If PulseChat is default SMS app and received via SMS_DELIVER, write to telephony provider
                        if (action == Telephony.Sms.Intents.SMS_DELIVER_ACTION || action == "android.provider.Telephony.SMS_DELIVERED") {
                            try {
                                val values = ContentValues().apply {
                                    put(Telephony.Sms.ADDRESS, sender)
                                    put(Telephony.Sms.BODY, fullBody)
                                    put(Telephony.Sms.DATE, System.currentTimeMillis())
                                    put(Telephony.Sms.READ, 0)
                                    put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
                                }
                                context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
                            } catch (_: Exception) {
                                // Ignored if permission/provider restricted
                            }
                        }
                    }
                }
            }
        }
    }
}
