package com.example.ui.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.receiver.NotificationActionReceiver

object NotificationHelper {

    private const val CHANNEL_ID = "pulsechat_messages_channel"
    private const val CHANNEL_NAME = "Messages"
    private const val CHANNEL_DESC = "Notifications for incoming text messages and chats"

    private const val DELIVERY_CHANNEL_ID = "pulsechat_delivery_channel"
    private const val DELIVERY_CHANNEL_NAME = "Delivery Reports"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
                lightColor = android.graphics.Color.CYAN
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            }

            val deliveryChannel = NotificationChannel(DELIVERY_CHANNEL_ID, DELIVERY_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW).apply {
                description = "SMS delivery confirmation reports"
                enableVibration(false)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
            notificationManager?.createNotificationChannel(deliveryChannel)
        }
    }

    fun showIncomingMessageNotification(
        context: Context,
        senderPhone: String,
        senderName: String?,
        messageText: String,
        vibratePattern: String = "NORMAL"
    ) {
        createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val displayName = senderName ?: senderPhone
        val notificationId = senderPhone.hashCode()

        // 1. Open conversation intent
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("conversationId", senderPhone)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Direct Reply Action (Inline)
        val remoteInput = RemoteInput.Builder(NotificationActionReceiver.KEY_TEXT_REPLY)
            .setLabel("Quick reply...")
            .build()

        val replyIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_QUICK_REPLY
            putExtra(NotificationActionReceiver.EXTRA_CONVERSATION_ID, senderPhone)
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
        )

        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            "Inline Reply",
            replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        // 3. Popup Big Screen Quick Reply Action
        val popupIntent = Intent(context, com.example.ui.quickreply.QuickReplyActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("conversationId", senderPhone)
            putExtra("senderPhone", senderPhone)
            putExtra("senderName", displayName)
            putExtra("initialMessage", messageText)
        }
        val popupPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 3,
            popupIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val popupAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_view,
            "Popup Reply 💬",
            popupPendingIntent
        ).build()

        // 4. Mark as Read Action
        val readIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_READ
            putExtra(NotificationActionReceiver.EXTRA_CONVERSATION_ID, senderPhone)
        }
        val readPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2,
            readIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val readAction = NotificationCompat.Action.Builder(
            android.R.drawable.checkbox_on_background,
            "Mark Read",
            readPendingIntent
        ).build()

        // Custom vibration pattern
        val vibrationWave = when (vibratePattern) {
            "SHORT" -> longArrayOf(0, 150)
            "LONG" -> longArrayOf(0, 500, 200, 500)
            "OFF" -> longArrayOf(0)
            else -> longArrayOf(0, 250, 150, 250) // NORMAL
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(displayName)
            .setContentText(messageText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(popupAction)
            .addAction(replyAction)
            .addAction(readAction)
            .setLights(android.graphics.Color.CYAN, 1000, 1000)
            .setVibrate(vibrationWave)

        // Enable Android / Samsung Floating Chat Bubble Metadata on Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val bubbleMetadata = NotificationCompat.BubbleMetadata.Builder(
                    popupPendingIntent,
                    androidx.core.graphics.drawable.IconCompat.createWithResource(context, com.example.R.mipmap.ic_launcher)
                )
                    .setDesiredHeight(600)
                    .setAutoExpandBubble(false)
                    .setSuppressNotification(false)
                    .build()
                builder.setBubbleMetadata(bubbleMetadata)
            } catch (_: Exception) {
                // Safe fallback if bubble icon resource differs
            }
        }

        val notification = builder.build()

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }

    fun showDeliveryReportNotification(
        context: Context,
        recipientPhone: String
    ) {
        createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val notificationId = ("delivery_$recipientPhone").hashCode()
        val text = "SMS to $recipientPhone was successfully delivered."

        val notification = NotificationCompat.Builder(context, DELIVERY_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentTitle("SMS Delivered")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setTimeoutAfter(8000) // auto dismiss in 8s
            .build()

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }
}
