package com.example.ui.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimeFormatter {
    fun formatMessageTimestamp(timestamp: Long): String {
        if (timestamp <= 0) return ""
        val now = Calendar.getInstance()
        val time = Calendar.getInstance().apply { timeInMillis = timestamp }

        val diffMillis = now.timeInMillis - timestamp
        val diffMinutes = diffMillis / (1000 * 60)

        if (diffMinutes < 1) return "Just now"

        val isSameDay = now.get(Calendar.YEAR) == time.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == time.get(Calendar.DAY_OF_YEAR)

        if (isSameDay) {
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            return timeFormat.format(Date(timestamp))
        }

        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = yesterday.get(Calendar.YEAR) == time.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == time.get(Calendar.DAY_OF_YEAR)

        if (isYesterday) return "Yesterday"

        val diffDays = diffMillis / (1000 * 3600 * 24)
        if (diffDays < 7) {
            val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
            return dayFormat.format(Date(timestamp))
        }

        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    fun formatDetailTimestamp(timestamp: Long): String {
        if (timestamp <= 0) return ""
        val timeFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        return timeFormat.format(Date(timestamp))
    }

    fun formatDateHeader(timestamp: Long): String {
        if (timestamp <= 0) return ""
        val now = Calendar.getInstance()
        val time = Calendar.getInstance().apply { timeInMillis = timestamp }

        val isSameDay = now.get(Calendar.YEAR) == time.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == time.get(Calendar.DAY_OF_YEAR)
        if (isSameDay) return "Today"

        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = yesterday.get(Calendar.YEAR) == time.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == time.get(Calendar.DAY_OF_YEAR)
        if (isYesterday) return "Yesterday"

        val isSameYear = now.get(Calendar.YEAR) == time.get(Calendar.YEAR)
        val format = if (isSameYear) {
            SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
        } else {
            SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
        }
        return format.format(Date(timestamp))
    }

    fun formatFullDateTime(timestamp: Long): String {
        if (timestamp <= 0) return ""
        val timeFormat = SimpleDateFormat("EEE, MMM d, yyyy h:mm:ss a", Locale.getDefault())
        return timeFormat.format(Date(timestamp))
    }
}
