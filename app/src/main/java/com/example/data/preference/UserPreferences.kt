package com.example.data.preference

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserProfile(
    val phoneNumber: String = "",
    val displayName: String = "",
    val avatarId: String = "avatar_1",
    val isOnboarded: Boolean = false,
    val themeMode: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    val colorTheme: String = "BLUE", // BLUE, TEAL, PURPLE, EMERALD
    val chatWallpaper: String = "NONE" // NONE, SUBTLE, DOODLE, NATURE
)

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("paichat_prefs", Context.MODE_PRIVATE)

    private val _userProfile = MutableStateFlow(loadProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private fun loadProfile(): UserProfile {
        val phone = prefs.getString("phone_number", "") ?: ""
        val name = prefs.getString("display_name", "") ?: ""
        val avatar = prefs.getString("avatar_id", "avatar_1") ?: "avatar_1"
        val onboarded = prefs.getBoolean("is_onboarded", false)
        val theme = prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM"
        val color = prefs.getString("color_theme", "BLUE") ?: "BLUE"
        val wallpaper = prefs.getString("chat_wallpaper", "NONE") ?: "NONE"
        return UserProfile(phone, name, avatar, onboarded, theme, color, wallpaper)
    }

    fun saveProfile(phoneNumber: String, displayName: String, avatarId: String) {
        val current = _userProfile.value
        prefs.edit()
            .putString("phone_number", phoneNumber)
            .putString("display_name", displayName)
            .putString("avatar_id", avatarId)
            .putBoolean("is_onboarded", true)
            .apply()
        _userProfile.value = current.copy(
            phoneNumber = phoneNumber,
            displayName = displayName,
            avatarId = avatarId,
            isOnboarded = true
        )
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
        _userProfile.value = _userProfile.value.copy(themeMode = mode)
    }

    fun setColorTheme(color: String) {
        prefs.edit().putString("color_theme", color).apply()
        _userProfile.value = _userProfile.value.copy(colorTheme = color)
    }

    fun setChatWallpaper(wallpaper: String) {
        prefs.edit().putString("chat_wallpaper", wallpaper).apply()
        _userProfile.value = _userProfile.value.copy(chatWallpaper = wallpaper)
    }

    fun clearProfile() {
        prefs.edit().clear().apply()
        _userProfile.value = UserProfile()
    }
}

