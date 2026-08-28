package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receiver for MMS WAP push notifications required for default SMS/MMS apps.
 */
class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // MMS handling stub for default SMS application compatibility
    }
}
