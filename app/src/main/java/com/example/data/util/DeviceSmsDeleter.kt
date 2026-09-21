package com.example.data.util

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import com.example.data.local.entity.MessageEntity
import com.example.ui.util.PhoneNumberUtil

/**
 * Utility for permanently removing SMS and MMS messages from Android's real device storage
 * (Telephony.Sms / Telephony.Mms Content Providers).
 */
object DeviceSmsDeleter {

    private const val TAG = "DeviceSmsDeleter"

    /**
     * Delete a single message from Android system SMS storage by ID or message metadata.
     */
    fun deleteMessageFromDevice(
        context: Context,
        message: MessageEntity
    ): Int {
        var deletedCount = 0
        val contentResolver = context.contentResolver

        try {
            // 1. Try deleting by numeric SMS ID (e.g., if messageId is "sms_12345" or "12345")
            val numericId = message.messageId.removePrefix("sms_").toLongOrNull()
            if (numericId != null && numericId > 0) {
                try {
                    val uri = ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, numericId)
                    val count = contentResolver.delete(uri, null, null)
                    if (count > 0) {
                        deletedCount += count
                        Log.d(TAG, "Deleted message $numericId from device storage by URI")
                        return deletedCount
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Could not delete by ID uri: ${e.message}")
                }
            }

            // 2. Fallback: match by phone number, content, and approximate timestamp (5s window)
            val phone = PhoneNumberUtil.normalize(
                if (message.senderPhoneNumber == "ME") message.recipientPhoneNumber else message.senderPhoneNumber
            )

            val whereParts = mutableListOf<String>()
            val args = mutableListOf<String>()

            if (phone.isNotBlank()) {
                whereParts.add("(${Telephony.Sms.ADDRESS} = ? OR ${Telephony.Sms.ADDRESS} LIKE ?)")
                args.add(phone)
                args.add("%$phone%")
            }

            if (message.content.isNotBlank()) {
                whereParts.add("${Telephony.Sms.BODY} = ?")
                args.add(message.content)
            }

            if (message.timestamp > 0) {
                whereParts.add("${Telephony.Sms.DATE} >= ? AND ${Telephony.Sms.DATE} <= ?")
                args.add((message.timestamp - 10000L).toString())
                args.add((message.timestamp + 10000L).toString())
            }

            if (whereParts.isNotEmpty()) {
                val count = contentResolver.delete(
                    Telephony.Sms.CONTENT_URI,
                    whereParts.joinToString(" AND "),
                    args.toTypedArray()
                )
                deletedCount += count
                Log.d(TAG, "Deleted $count message(s) by metadata match from device storage")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete message from device storage: ${e.message}")
        }

        return deletedCount
    }

    /**
     * Delete multiple messages from Android system SMS storage.
     */
    fun deleteMessagesFromDevice(
        context: Context,
        messages: List<MessageEntity>
    ): Int {
        var totalDeleted = 0
        for (msg in messages) {
            totalDeleted += deleteMessageFromDevice(context, msg)
        }
        return totalDeleted
    }

    /**
     * Delete all messages for a specific conversation phone number from Android system SMS storage.
     */
    fun deleteConversationFromDevice(
        context: Context,
        phoneNumber: String
    ): Int {
        var deletedCount = 0
        try {
            val contentResolver = context.contentResolver
            val normalizedPhone = PhoneNumberUtil.normalize(phoneNumber)

            if (normalizedPhone.isNotBlank()) {
                deletedCount = contentResolver.delete(
                    Telephony.Sms.CONTENT_URI,
                    "${Telephony.Sms.ADDRESS} = ? OR ${Telephony.Sms.ADDRESS} LIKE ?",
                    arrayOf(normalizedPhone, "%$normalizedPhone%")
                )
                Log.d(TAG, "Deleted $deletedCount SMS entries for $normalizedPhone from device storage")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete conversation from device storage: ${e.message}")
        }
        return deletedCount
    }
}
