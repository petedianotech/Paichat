package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Telephony
import android.util.Log
import com.example.data.local.database.PulseChatDatabase
import com.example.data.local.entity.MessageType
import com.example.data.preference.UserPreferences
import com.example.data.repository.ContactRepository
import com.example.data.repository.MessageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receiver for MMS WAP push notifications and MMS delivery.
 * Compliant with Android Default SMS/MMS application specifications.
 * Uses goAsync() and WakeLock for guaranteed processing during background / low-power states.
 */
class MmsReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "MmsReceiver"
        private const val WAKELOCK_TAG = "PulseChat:MmsReceiverWakeLock"
        private const val WAKELOCK_TIMEOUT_MS = 20000L // 20 seconds
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "MmsReceiver received action: $action")

        if (action == Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION ||
            action == Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION
        ) {
            val mimeType = intent.type
            if (mimeType == "application/vnd.wap.mms-message") {
                val pdu = intent.getByteArrayExtra("data")

                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG)?.apply {
                    setReferenceCounted(false)
                    acquire(WAKELOCK_TIMEOUT_MS)
                }

                val pendingResult = goAsync()

                val db = PulseChatDatabase.getDatabase(context)
                val contactRepo = ContactRepository()
                val prefs = UserPreferences(context)
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
                        // Extract sender address from PDU if available or fallback
                        val sender = extractSenderFromPdu(pdu) ?: "Unknown Sender"
                        val contentText = "[MMS Multimedia Message]"

                        if (com.example.ui.util.MessageDeduplicator.isDuplicateAndMark(sender, contentText)) {
                            Log.d(TAG, "Duplicate incoming MMS detected from $sender - ignoring")
                            return@launch
                        }

                        messageRepo.receiveIncomingMessage(
                            senderPhone = sender,
                            senderName = null,
                            content = contentText,
                            messageType = MessageType.MMS
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to process incoming MMS: ${e.message}", e)
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

    private fun extractSenderFromPdu(pdu: ByteArray?): String? {
        if (pdu == null || pdu.isEmpty()) return null
        try {
            // Find phone number pattern or headers in raw PDU bytes
            val str = String(pdu, Charsets.ISO_8859_1)
            val phoneRegex = Regex("""(\+?[0-9]{7,15})""")
            val match = phoneRegex.find(str)
            if (match != null) {
                return match.value
            }
        } catch (_: Exception) {}
        return null
    }
}
