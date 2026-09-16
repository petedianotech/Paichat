package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Standard MmsReceiver declared for Android default SMS app capability validation.
 * Since PaiChat is SMS-only, this receiver does not handle any MMS payload processing,
 * but is required by the Android OS to allow setting the app as the default SMS handler.
 */
class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // No-op: MMS compatibility only
    }
}
