package com.example.ui.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R
import com.example.receiver.NotificationActionReceiver
import com.example.service.ChatHeadService

object NotificationHelper {

    private const val CHANNEL_ID = "pulsechat_messages_channel"
    private const val CHANNEL_NAME = "Messages"
    private const val CHANNEL_DESC = "Notifications for incoming text messages and chats"

    private const val DELIVERY_CHANNEL_ID = "pulsechat_delivery_channel"
    private const val DELIVERY_CHANNEL_NAME = "Delivery Reports"
    private const val DELIVERY_CHANNEL_DESC = "SMS delivery confirmation reports"

    private const val SHORTCUT_CATEGORY = "com.aistudio.paichat.category.TEXT_SHARE_TARGET"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
                lightColor = android.graphics.Color.CYAN
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
                setShowBadge(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAllowBubbles(true)
                }
            }

            val deliveryChannel = NotificationChannel(
                DELIVERY_CHANNEL_ID,
                DELIVERY_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = DELIVERY_CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
                lightColor = android.graphics.Color.parseColor("#2563EB")
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
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
        val shortcutId = "chat_$senderPhone"

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

        val popupIntent = Intent(context, com.example.ui.quickreply.QuickReplyActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("conversationId", senderPhone)
            putExtra("senderPhone", senderPhone)
            putExtra("senderName", displayName)
            putExtra("initialMessage", messageText)
            action = Intent.ACTION_VIEW
        }
        val popupPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 3,
            popupIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val popupAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_view,
            "Popup Reply",
            popupPendingIntent
        ).build()

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

        val vibrationWave = when (vibratePattern) {
            "SHORT" -> longArrayOf(0, 150)
            "LONG" -> longArrayOf(0, 500, 200, 500)
            "OFF" -> longArrayOf(0)
            else -> longArrayOf(0, 250, 150, 250)
        }

        val bubbleIcon = IconCompat.createWithResource(context, R.mipmap.ic_launcher)
        var largeBitmap: android.graphics.Bitmap? = null
        try {
            val contactRepo = com.example.data.repository.ContactRepository()
            val photoUriStr = contactRepo.resolveContactPhotoUri(context, senderPhone)
            if (!photoUriStr.isNullOrBlank()) {
                val uri = android.net.Uri.parse(photoUriStr)
                largeBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                    android.graphics.ImageDecoder.decodeBitmap(source)
                } else {
                    @Suppress("DEPRECATION")
                    android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
            }
        } catch (_: Exception) {
        }

        val personBuilder = Person.Builder()
            .setName(displayName)
            .setKey(senderPhone)
            .setImportant(true)
        if (largeBitmap != null) {
            personBuilder.setIcon(IconCompat.createWithBitmap(largeBitmap))
        }
        val person = personBuilder.build()

        // Dynamic conversation shortcut required for reliable system bubbles (Android 11+)
        try {
            val shortcut = ShortcutInfoCompat.Builder(context, shortcutId)
                .setShortLabel(displayName)
                .setLongLabel(displayName)
                .setIcon(if (largeBitmap != null) IconCompat.createWithBitmap(largeBitmap) else bubbleIcon)
                .setLongLived(true)
                .setPerson(person)
                .setCategories(setOf(SHORTCUT_CATEGORY))
                .setIntent(
                    Intent(context, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        putExtra("conversationId", senderPhone)
                    }
                )
                .build()
            ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
        } catch (e: Exception) {
            android.util.Log.w("NotificationHelper", "Shortcut push failed: ${e.message}")
        }

        val messagingStyle = NotificationCompat.MessagingStyle(person)
            .setConversationTitle(displayName)
            .addMessage(messageText, System.currentTimeMillis(), person)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setColor(android.graphics.Color.parseColor("#1688F5"))
            .setContentTitle(displayName)
            .setContentText(messageText)
            .setStyle(messagingStyle)
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
            .setShortcutId(shortcutId)
            .addPerson(person)

        if (largeBitmap != null) {
            builder.setLargeIcon(largeBitmap)
        }

        // Official system bubble (draggable on Android 11+ when user allows bubbles)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val iconForBubble = if (largeBitmap != null) {
                    IconCompat.createWithBitmap(largeBitmap)
                } else {
                    bubbleIcon
                }
                val bubbleMetadata = NotificationCompat.BubbleMetadata.Builder(
                    popupPendingIntent,
                    iconForBubble
                )
                    .setDesiredHeight(640)
                    .setAutoExpandBubble(false)
                    .setSuppressNotification(false)
                    .build()
                builder.setBubbleMetadata(bubbleMetadata)
            } catch (e: Exception) {
                android.util.Log.w("NotificationHelper", "BubbleMetadata failed: ${e.message}")
            }
        }

        val notification = builder.build()
        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(notificationId, notification)
        } catch (_: SecurityException) {
        }

        // Messenger-style overlay chat head (always works if user granted "Display over other apps")
        try {
            ChatHeadService.start(context, senderPhone, displayName, messageText)
        } catch (e: Exception) {
            android.util.Log.w("NotificationHelper", "ChatHead start failed: ${e.message}")
        }
    }

    fun launchQuickReplyPopup(
        context: Context,
        senderPhone: String,
        senderName: String?,
        messageText: String
    ) {
        try {
            val popupIntent = Intent(context, com.example.ui.quickreply.QuickReplyActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                putExtra("conversationId", senderPhone)
                putExtra("senderPhone", senderPhone)
                putExtra("senderName", senderName ?: senderPhone)
                putExtra("initialMessage", messageText)
            }
            context.startActivity(popupIntent)
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Could not start QuickReplyActivity: ${e.message}")
        }
    }

    fun showDeliveryReportNotification(
        context: Context,
        recipientPhone: String,
        recipientName: String? = null,
        messageContent: String? = null,
        durationSetting: String = "10_SECONDS"
    ) {
        createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val displayName = (recipientName?.takeIf { it.isNotBlank() } ?: recipientPhone).ifBlank { "Recipient" }
        val notificationId = ("delivery_${recipientPhone}").hashCode()

        val shortContent = if (!messageContent.isNullOrBlank()) {
            "Delivered to $displayName: \"${messageContent.take(60)}${if (messageContent.length > 60) "…" else ""}\""
        } else {
            "SMS to $displayName was delivered successfully"
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("conversationId", recipientPhone)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val openPendingIntent = PendingIntent.getActivity(context, notificationId, openIntent, flags)

        val notificationBuilder = NotificationCompat.Builder(context, DELIVERY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_delivered)
            .setContentTitle("Delivered • $displayName")
            .setContentText(shortContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(shortContent))
            .setColor(0xFF2563EB.toInt())
            .setContentIntent(openPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setLights(android.graphics.Color.parseColor("#2563EB"), 800, 800)

        if (durationSetting == "10_SECONDS") {
            notificationBuilder.setTimeoutAfter(10_000L)
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notificationBuilder.build())
        } catch (_: Exception) {
        }
    }

    fun showSendingTimeoutNotification(
        context: Context,
        recipientPhone: String,
        recipientName: String?,
        messageId: String,
        content: String,
        isAutoRetrying: Boolean
    ) {
        createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val displayName = recipientName ?: recipientPhone
        val notificationId = messageId.hashCode()

        val retryIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_RETRY_SEND
            putExtra(NotificationActionReceiver.EXTRA_MESSAGE_ID, messageId)
            putExtra(NotificationActionReceiver.EXTRA_CONVERSATION_ID, recipientPhone)
        }
        val retryPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 5,
            retryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val retryAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_rotate,
            if (isAutoRetrying) "Auto-Retrying" else "Retry",
            retryPendingIntent
        ).build()

        val cancelIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_CANCEL_RETRY
            putExtra(NotificationActionReceiver.EXTRA_MESSAGE_ID, messageId)
            putExtra(NotificationActionReceiver.EXTRA_CONVERSATION_ID, recipientPhone)
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 10,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Cancel Send",
            cancelPendingIntent
        ).build()

        val statusMsg = if (isAutoRetrying) {
            "SMS is taking over 60s to send. Auto-retrying..."
        } else {
            "SMS pending over 60s. Tap Retry to send."
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("SMS Delivery Pending ($displayName)")
            .setContentText(statusMsg)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$statusMsg\n\nMessage: $content"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .addAction(retryAction)
            .addAction(cancelAction)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
        }
    }
}
