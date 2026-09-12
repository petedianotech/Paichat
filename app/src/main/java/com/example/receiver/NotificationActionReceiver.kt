package com.example.receiver

import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.data.local.database.PulseChatDatabase
import com.example.data.repository.ContactRepository
import com.example.data.repository.MessageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_READ = "com.example.ACTION_MARK_READ"
        const val ACTION_QUICK_REPLY = "com.example.ACTION_QUICK_REPLY"
        const val ACTION_RETRY_SEND = "com.example.ACTION_RETRY_SEND"
        const val ACTION_CANCEL_RETRY = "com.example.ACTION_CANCEL_RETRY"
        const val EXTRA_CONVERSATION_ID = "extra_conversation_id"
        const val EXTRA_MESSAGE_ID = "extra_message_id"
        const val KEY_TEXT_REPLY = "key_text_reply"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID) ?: return

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

        val notificationManager = NotificationManagerCompat.from(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_MARK_READ -> {
                        messageRepo.clearUnreadCount(conversationId)
                        notificationManager.cancel(conversationId.hashCode())
                    }

                    ACTION_QUICK_REPLY -> {
                        val remoteInput = RemoteInput.getResultsFromIntent(intent)
                        val replyText = remoteInput?.getCharSequence(KEY_TEXT_REPLY)?.toString()

                        if (!replyText.isNullOrBlank()) {
                            messageRepo.clearUnreadCount(conversationId)
                            messageRepo.sendMessage(
                                recipientPhone = conversationId,
                                recipientName = null,
                                content = replyText.trim()
                            )
                            notificationManager.cancel(conversationId.hashCode())
                        }
                    }

                    ACTION_RETRY_SEND -> {
                        val messageId = intent.getStringExtra(EXTRA_MESSAGE_ID)
                        if (!messageId.isNullOrBlank()) {
                            messageRepo.retryMessage(messageId)
                            notificationManager.cancel(messageId.hashCode())
                        }
                    }

                    ACTION_CANCEL_RETRY -> {
                        val messageId = intent.getStringExtra(EXTRA_MESSAGE_ID)
                        if (!messageId.isNullOrBlank()) {
                            messageRepo.cancelRetry(messageId)
                            notificationManager.cancel(messageId.hashCode())
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
