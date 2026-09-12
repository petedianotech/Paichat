package com.example.service

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.telephony.TelephonyManager
import android.util.Log
import com.example.data.local.database.PulseChatDatabase
import com.example.data.repository.ContactRepository
import com.example.data.repository.MessageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Service for sending quick responses without opening the UI, required for default SMS apps.
 * Handles TelephonyManager.ACTION_RESPOND_VIA_MESSAGE (e.g., when declining an incoming call
 * with a quick SMS text response).
 */
class HeadlessSmsSendService : Service() {

    companion object {
        private const val TAG = "HeadlessSmsSendService"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val action = intent.action
        Log.d(TAG, "onStartCommand action: $action")

        if (TelephonyManager.ACTION_RESPOND_VIA_MESSAGE == action) {
            val extras = intent.extras
            if (extras == null) {
                stopSelf(startId)
                return START_NOT_STICKY
            }

            val messageText = extras.getString(Intent.EXTRA_TEXT)
            val uri = intent.data
            val recipient = getRecipient(uri)

            if (!messageText.isNullOrBlank() && !recipient.isNullOrBlank()) {
                Log.d(TAG, "Sending headless quick-response SMS to $recipient: $messageText")
                val db = PulseChatDatabase.getDatabase(applicationContext)
                val contactRepo = ContactRepository()
                val prefs = com.example.data.preference.UserPreferences(applicationContext)
                val messageRepo = MessageRepository(
                    applicationContext,
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
                        messageRepo.sendMessage(
                            recipientPhone = recipient,
                            recipientName = null,
                            content = messageText.trim()
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send headless SMS: ${e.message}", e)
                    } finally {
                        stopSelf(startId)
                    }
                }
                return START_NOT_STICKY
            }
        }

        stopSelf(startId)
        return START_NOT_STICKY
    }

    private fun getRecipient(uri: Uri?): String? {
        if (uri == null) return null
        val scheme = uri.scheme
        if (scheme == "sms" || scheme == "smsto" || scheme == "mms" || scheme == "mmsto") {
            val recipient = uri.schemeSpecificPart
            val index = recipient.indexOf('?')
            return if (index != -1) {
                recipient.substring(0, index)
            } else {
                recipient
            }
        }
        return null
    }
}
