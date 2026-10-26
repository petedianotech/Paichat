package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ui.quickreply.QuickReplyActivity
import kotlin.math.abs

/**
 * Floating chat-head style bubble over other apps (requires SYSTEM_ALERT_WINDOW).
 * Drag anywhere; tap opens QuickReplyActivity for that conversation.
 * Drag toward bottom to dismiss.
 */
class ChatHeadService : Service() {

    companion object {
        const val EXTRA_PHONE = "extra_phone"
        const val EXTRA_NAME = "extra_name"
        const val EXTRA_MESSAGE = "extra_message"
        private const val FG_CHANNEL = "paichat_chat_head_fg"
        private const val FG_ID = 99001

        fun canDrawOverlays(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else true
        }

        fun start(context: Context, phone: String, name: String?, message: String) {
            if (!canDrawOverlays(context)) return
            val i = Intent(context, ChatHeadService::class.java).apply {
                putExtra(EXTRA_PHONE, phone)
                putExtra(EXTRA_NAME, name ?: phone)
                putExtra(EXTRA_MESSAGE, message)
            }
            ContextCompat.startForegroundService(context, i)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ChatHeadService::class.java))
        }
    }

    private var windowManager: WindowManager? = null
    private var bubbleView: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var phone: String = ""
    private var displayName: String = ""
    private var message: String = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        phone = intent?.getStringExtra(EXTRA_PHONE) ?: ""
        displayName = intent?.getStringExtra(EXTRA_NAME) ?: phone
        message = intent?.getStringExtra(EXTRA_MESSAGE) ?: ""

        startAsForeground()
        if (phone.isNotBlank() && canDrawOverlays(this)) {
            showBubble()
        } else {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startAsForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(
                NotificationChannel(FG_CHANNEL, "Floating chat head", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Keeps the reply bubble on screen"
                    setShowBadge(false)
                }
            )
        }
        val notif = NotificationCompat.Builder(this, FG_CHANNEL)
            .setContentTitle("Chat bubble active")
            .setContentText(displayName.ifBlank { "PaiChat" })
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        startForeground(FG_ID, notif)
    }

    private fun showBubble() {
        if (bubbleView != null) {
            // Update existing for same/new conversation
            removeBubble()
        }
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val size = dp(56)
        val container = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(size, size)
        }

        val bg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(0xFF1688F5.toInt())
        }
        val avatar = ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher)
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = bg
            clipToOutline = true
            outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
            layoutParams = FrameLayout.LayoutParams(size, size)
        }
        // Initial letter badge overlay
        val letter = TextView(this).apply {
            text = displayName.take(1).uppercase()
            setTextColor(0xFFFFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(size, size)
        }
        container.addView(avatar)
        container.addView(letter)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            size,
            size,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(12)
            y = dp(120)
        }

        var downX = 0f
        var downY = 0f
        var paramX = 0
        var paramY = 0
        var moved = false

        container.setOnTouchListener { _, event ->
            val p = params ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    paramX = p.x
                    paramY = p.y
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downX).toInt()
                    val dy = (event.rawY - downY).toInt()
                    if (abs(dx) > 8 || abs(dy) > 8) moved = true
                    p.x = paramX + dx
                    p.y = paramY + dy
                    try {
                        windowManager?.updateViewLayout(container, p)
                    } catch (_: Exception) {}
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        openReply()
                    } else {
                        // Snap to edge; dismiss if near bottom
                        val dm = resources.displayMetrics
                        if (p.y > dm.heightPixels - dp(140)) {
                            stopSelf()
                        } else {
                            p.x = if (p.x + size / 2 < dm.widthPixels / 2) dp(8) else dm.widthPixels - size - dp(8)
                            try {
                                windowManager?.updateViewLayout(container, p)
                            } catch (_: Exception) {}
                        }
                    }
                    true
                }
                else -> false
            }
        }

        bubbleView = container
        try {
            windowManager?.addView(container, params)
        } catch (e: Exception) {
            android.util.Log.e("ChatHeadService", "addView failed: ${e.message}")
            stopSelf()
        }
    }

    private fun openReply() {
        val intent = Intent(this, QuickReplyActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra("conversationId", phone)
            putExtra("senderPhone", phone)
            putExtra("senderName", displayName)
            putExtra("initialMessage", message)
        }
        startActivity(intent)
        // Keep bubble so user can re-open; stop if you prefer one-shot:
        // stopSelf()
    }

    private fun removeBubble() {
        try {
            bubbleView?.let { windowManager?.removeView(it) }
        } catch (_: Exception) {}
        bubbleView = null
    }

    private fun dp(v: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

    override fun onDestroy() {
        removeBubble()
        super.onDestroy()
    }
}
