package com.example.receiver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.database.PulseChatDatabase
import com.example.data.local.entity.MessageStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val uri = intent.data ?: return
        val messageId = uri.schemeSpecificPart ?: return

        val db = PulseChatDatabase.getDatabase(context)
        val messageDao = db.messageDao()

        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                if (action == "com.example.SMS_SENT") {
                    val status = if (resultCode == Activity.RESULT_OK) {
                        MessageStatus.SENT
                    } else {
                        MessageStatus.FAILED
                    }
                    messageDao.updateMessageStatus(messageId, status)
                } else if (action == "com.example.SMS_DELIVERED") {
                    messageDao.updateMessageStatus(messageId, MessageStatus.DELIVERED)
                }
            } catch (e: Exception) {
                Log.e("SmsStatusReceiver", "Failed to update status for message $messageId: ${e.message}")
            }
        }
    }
}
