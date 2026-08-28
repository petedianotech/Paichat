package com.example.ui.util

import androidx.compose.ui.graphics.Color

object AvatarUtil {
    val avatarPresets = listOf(
        "avatar_1" to "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80",
        "avatar_2" to "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=200&q=80",
        "avatar_3" to "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=200&q=80",
        "avatar_4" to "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=200&q=80",
        "avatar_5" to "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=200&q=80"
    )

    fun getAvatarUrl(avatarId: String): String {
        return avatarPresets.find { it.first == avatarId }?.second ?: avatarPresets.first().second
    }

    fun getInitials(name: String?): String {
        if (name.isNullOrBlank()) return "?"
        val parts = name.trim().split(" ").filter { it.isNotBlank() }
        return when {
            parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
            parts.isNotEmpty() -> parts[0].first().uppercaseChar().toString()
            else -> "?"
        }
    }

    private val avatarColors = listOf(
        Color(0xFF1E88E5),
        Color(0xFFD81B60),
        Color(0xFF8E24AA),
        Color(0xFF00897B),
        Color(0xFFF4511E),
        Color(0xFF3949AB),
        Color(0xFF00ACC1),
        Color(0xFF43A047)
    )

    fun getAvatarColor(key: String): Color {
        val hash = key.hashCode()
        val index = kotlin.math.abs(hash) % avatarColors.size
        return avatarColors[index]
    }
}
