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
    val isOnboarded: Boolean = false
)

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("pulse_chat_prefs", Context.MODE_PRIVATE)

    private val _userProfile = MutableStateFlow(loadProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private fun loadProfile(): UserProfile {
        val phone = prefs.getString("phone_number", "") ?: ""
        val name = prefs.getString("display_name", "") ?: ""
        val avatar = prefs.getString("avatar_id", "avatar_1") ?: "avatar_1"
        val onboarded = prefs.getBoolean("is_onboarded", false)
        return UserProfile(phone, name, avatar, onboarded)
    }

    fun saveProfile(phoneNumber: String, displayName: String, avatarId: String) {
        prefs.edit()
            .putString("phone_number", phoneNumber)
            .putString("display_name", displayName)
            .putString("avatar_id", avatarId)
            .putBoolean("is_onboarded", true)
            .apply()
        _userProfile.value = UserProfile(phoneNumber, displayName, avatarId, true)
    }

    fun clearProfile() {
        prefs.edit().clear().apply()
        _userProfile.value = UserProfile()
    }
}
