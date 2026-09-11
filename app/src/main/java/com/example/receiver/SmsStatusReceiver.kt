package com.example.receiver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.database.PulseChatDatabase
import com.example.data.local.entity.MessageStatus
import com.example.data.preference.UserPreferences
import com.example.ui.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val uri = intent.data ?: return
        val messageId = uri.host?.takeIf { it.isNotBlank() }
            ?: uri.schemeSpecificPart?.removePrefix("//")?.takeIf { it.isNotBlank() }
            ?: uri.schemeSpecificPart ?: return

        val resultCodeCopy = resultCode
        val pendingResult = goAsync()

        val db = PulseChatDatabase.getDatabase(context)
        val messageDao = db.messageDao()
        val prefs = UserPreferences(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (action == "com.example.SMS_SENT") {
                    val status = if (resultCodeCopy == Activity.RESULT_OK) {
                        MessageStatus.SENT
                    } else {
                        MessageStatus.FAILED
                    }
                    messageDao.updateMessageStatus(messageId, status)
                } else if (action == "com.example.SMS_DELIVERED") {
                    messageDao.updateMessageStatus(messageId, MessageStatus.DELIVERED)

                    // Delivery Report Notification if customizable setting is enabled (BOTH or NOTIFICATIONS_ONLY)
                    val settings = prefs.appSettings.value
                    val shouldNotify = settings.deliveryReportMode in listOf("BOTH", "NOTIFICATIONS_ONLY") ||
                            (settings.deliveryReportMode == "DEFAULT" && settings.deliveryReports)

                    if (shouldNotify) {
                        val message = messageDao.getMessageById(messageId)
                        if (message != null) {
                            NotificationHelper.showDeliveryReportNotification(
                                context = context,
                                recipientPhone = message.recipientPhoneNumber
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SmsStatusReceiver", "Failed to update status for message $messageId: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
