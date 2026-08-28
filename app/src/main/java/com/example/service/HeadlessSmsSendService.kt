package com.example.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Service for sending quick responses without opening the UI, required for default SMS apps.
 */
class HeadlessSmsSendService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stopSelf(startId)
        return START_NOT_STICKY
    }
}
