package com.example.ui.util

import androidx.compose.ui.graphics.Color

object AvatarUtil {
    fun getInitials(name: String?): String {
        if (name.isNullOrBlank()) return "#"
        val clean = name.trim()
        val parts = clean.split(Regex("""\s+""")).filter { it.isNotBlank() }
        return try {
            when {
                parts.size >= 2 -> {
                    val first = parts[0].firstOrNull { it.isLetterOrDigit() } ?: parts[0].firstOrNull() ?: '#'
                    val second = parts[1].firstOrNull { it.isLetterOrDigit() } ?: parts[1].firstOrNull() ?: '#'
                    "${first.uppercaseChar()}${second.uppercaseChar()}"
                }
                parts.isNotEmpty() -> {
                    val first = parts[0].firstOrNull { it.isLetterOrDigit() } ?: parts[0].firstOrNull() ?: '#'
                    first.uppercaseChar().toString()
                }
                else -> "#"
            }
        } catch (_: Exception) {
            "#"
        }
    }

    // Modern, vibrant Material colors (Zero greens)
    private val avatarColors = listOf(
        Color(0xFF2563EB), // Royal Blue
        Color(0xFF4F46E5), // Indigo
        Color(0xFF7C3AED), // Violet
        Color(0xFF9333EA), // Purple
        Color(0xFFC026D3), // Fuchsia
        Color(0xFFE11D48), // Rose
        Color(0xFFEA580C), // Orange / Amber
        Color(0xFF0284C7), // Sky Blue
        Color(0xFF0891B2), // Cyan
        Color(0xFF475569)  // Slate
    )

    fun getAvatarColor(key: String): Color {
        val hash = key.hashCode()
        val index = (hash and 0x7FFFFFFF) % avatarColors.size
        return avatarColors[index]
    }
}
