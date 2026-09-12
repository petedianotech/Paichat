package com.example.receiver

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Telephony
import android.util.Log
import com.example.data.local.database.PulseChatDatabase
import com.example.data.local.entity.MessageType
import com.example.data.repository.ContactRepository
import com.example.data.repository.MessageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * High-reliability BroadcastReceiver for incoming SMS messages.
 * Uses goAsync() and a short-duration WakeLock to ensure Android doesn't kill the process
 * before the SMS is written to Room DB and the notification is dispatched.
 */
class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
        private const val WAKELOCK_TAG = "PulseChat:SmsReceiverWakeLock"
        private const val WAKELOCK_TIMEOUT_MS = 15000L // 15 seconds max
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "onReceive action: $action")

        if (action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION ||
            action == Telephony.Sms.Intents.SMS_DELIVER_ACTION ||
            action == "android.provider.Telephony.SMS_DELIVERED"
        ) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) {
                Log.d(TAG, "No SMS messages extracted from intent")
                return
            }

            // Keep CPU awake until coroutine completes processing
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG)?.apply {
                setReferenceCounted(false)
                acquire(WAKELOCK_TIMEOUT_MS)
            }

            val pendingResult = goAsync()

            val db = PulseChatDatabase.getDatabase(context)
            val contactRepo = ContactRepository()
            val prefs = com.example.data.preference.UserPreferences(context)
            val messageRepo = MessageRepository(
                context,
                db.conversationDao(),
                db.messageDao(),
                db.scheduledMessageDao(),
                db.blockedContactDao(),
                db.quickResponseDao(),
                contactRepo,
                prefs
            )

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Group multipart messages by originating address
                    val senderToBodyMap = mutableMapOf<String, StringBuilder>()
                    for (sms in messages) {
                        val sender = sms.originatingAddress ?: continue
                        val body = sms.messageBody ?: ""
                        val sb = senderToBodyMap.getOrPut(sender) { StringBuilder() }
                        sb.append(body)
                    }

                    for ((sender, bodyBuilder) in senderToBodyMap) {
                        val fullBody = bodyBuilder.toString()
                        Log.d(TAG, "Processing incoming SMS from $sender (${fullBody.length} chars)")

                        messageRepo.receiveIncomingMessage(
                            senderPhone = sender,
                            senderName = null,
                            content = fullBody,
                            messageType = MessageType.SMS
                        )

                        // If PulseChat is default SMS app and received via SMS_DELIVER, persist to system telephony provider
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
                            } catch (e: Exception) {
                                Log.w(TAG, "Could not insert to Telephony.Sms.Inbox: ${e.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling incoming SMS: ${e.message}", e)
                } finally {
                    try {
                        if (wakeLock?.isHeld == true) {
                            wakeLock.release()
                        }
                    } catch (_: Exception) {}
                    pendingResult.finish()
                }
            }
        }
    }
}
