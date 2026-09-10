package com.example.ui.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import com.example.data.model.SimCardInfo

object SimManagerHelper {

    fun getActiveSimCards(context: Context): List<SimCardInfo> {
        val simList = mutableListOf<SimCardInfo>()
        val hasPhoneState = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
                if (subscriptionManager != null && hasPhoneState) {
                    val activeSubscriptions: List<SubscriptionInfo>? = try {
                        subscriptionManager.activeSubscriptionInfoList
                    } catch (_: SecurityException) {
                        null
                    }

                    if (!activeSubscriptions.isNullOrEmpty()) {
                        for (subInfo in activeSubscriptions) {
                            simList.add(
                                SimCardInfo(
                                    slotIndex = subInfo.simSlotIndex,
                                    subscriptionId = subInfo.subscriptionId,
                                    displayName = subInfo.displayName?.toString() ?: "SIM ${subInfo.simSlotIndex + 1}",
                                    carrierName = subInfo.carrierName?.toString() ?: "SIM ${subInfo.simSlotIndex + 1}",
                                    number = subInfo.number
                                )
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Safe fallback
        }

        // Default SIM fallback if no multiple SIMs were detected
        if (simList.isEmpty()) {
            simList.add(
                SimCardInfo(
                    slotIndex = 0,
                    subscriptionId = -1,
                    displayName = "SIM 1 (Default)",
                    carrierName = "Default Carrier"
                )
            )
        }

        return simList
    }
}
