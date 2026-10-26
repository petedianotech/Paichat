package com.example.data.preference

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val isOnboarded: Boolean = false,
    val isTermsAccepted: Boolean = false,
    val themeMode: String = "SYSTEM",
    val colorTheme: String = "BLUE",
    val sendDelaySeconds: Int = 3,
    val deliveryReports: Boolean = true,
    val deliveryReportMode: String = "BOTH",
    val deliveryReportDuration: String = "10_SECONDS",
    val signatureText: String = "",
    val bubbleShape: String = "ROUNDED",
    val fontSize: String = "NORMAL",
    val fontFamily: String = "DEFAULT",
    val vibrateOnSend: Boolean = true,
    val showCharacterCounter: Boolean = true,
    val selectedSimSlot: Int = 0,
    val notificationSound: Boolean = true,
    val notificationVibratePattern: String = "NORMAL",
    val quickReplyPopup: Boolean = true,
    val popupPreviewSize: String = "STANDARD",
    val autoRetryAfterTimeout: Boolean = true,
    val repeatNotificationCount: Int = 0,
    val userName: String = "You",
    val userPhoneNumber: String = "",
    val userAvatarColor: String = "#005AC1",
    val userStatus: String = "SMS Messenger • Fast & Secure",
    val lastSmsSyncTimestamp: Long = 0L,
    val defaultChatWallpaper: String = "NONE",
    val mmsImageCompressionQuality: String = "NORMAL",
    val swipeGesturesEnabled: Boolean = true
)

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences("sms_app_prefs", Context.MODE_PRIVATE)

    companion object {
        @Volatile
        private var sharedSettingsFlow: MutableStateFlow<AppSettings>? = null
        private var preferenceListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

        private fun getOrCreateFlow(prefs: SharedPreferences): MutableStateFlow<AppSettings> {
            return sharedSettingsFlow ?: synchronized(this) {
                sharedSettingsFlow ?: run {
                    val initial = loadSettings(prefs)
                    val flow = MutableStateFlow(initial)
                    sharedSettingsFlow = flow
                    val listener = SharedPreferences.OnSharedPreferenceChangeListener { sp, _ ->
                        flow.value = loadSettings(sp)
                    }
                    preferenceListener = listener
                    prefs.registerOnSharedPreferenceChangeListener(listener)
                    flow
                }
            }
        }

        private fun loadSettings(prefs: SharedPreferences): AppSettings {
            return AppSettings(
                isOnboarded = prefs.getBoolean("is_onboarded", false),
                isTermsAccepted = prefs.getBoolean("has_accepted_terms", false),
                themeMode = prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM",
                colorTheme = prefs.getString("color_theme", "BLUE") ?: "BLUE",
                sendDelaySeconds = prefs.getInt("send_delay_seconds", 3),
                deliveryReports = prefs.getBoolean("delivery_reports", true),
                deliveryReportMode = prefs.getString("delivery_report_mode", "BOTH") ?: "BOTH",
                deliveryReportDuration = prefs.getString("delivery_report_duration", "10_SECONDS") ?: "10_SECONDS",
                signatureText = prefs.getString("signature_text", "") ?: "",
                bubbleShape = prefs.getString("bubble_shape", "ROUNDED") ?: "ROUNDED",
                fontSize = prefs.getString("font_size", "NORMAL") ?: "NORMAL",
                fontFamily = prefs.getString("font_family", "DEFAULT") ?: "DEFAULT",
                vibrateOnSend = prefs.getBoolean("vibrate_on_send", true),
                showCharacterCounter = prefs.getBoolean("show_char_counter", true),
                selectedSimSlot = prefs.getInt("selected_sim_slot", 0),
                notificationSound = prefs.getBoolean("notification_sound", true),
                notificationVibratePattern = prefs.getString("notification_vibrate_pattern", "NORMAL") ?: "NORMAL",
                quickReplyPopup = prefs.getBoolean("quick_reply_popup", true),
                popupPreviewSize = prefs.getString("popup_preview_size", "STANDARD") ?: "STANDARD",
                autoRetryAfterTimeout = prefs.getBoolean("auto_retry_after_timeout", true),
                repeatNotificationCount = prefs.getInt("repeat_notification_count", 0),
                userName = prefs.getString("user_name", "You") ?: "You",
                userPhoneNumber = prefs.getString("user_phone_number", "") ?: "",
                userAvatarColor = prefs.getString("user_avatar_color", "#005AC1") ?: "#005AC1",
                userStatus = prefs.getString("user_status", "SMS Messenger • Fast & Secure") ?: "SMS Messenger • Fast & Secure",
                lastSmsSyncTimestamp = prefs.getLong("last_sms_sync_timestamp", 0L),
                defaultChatWallpaper = prefs.getString("default_chat_wallpaper", "NONE") ?: "NONE",
                mmsImageCompressionQuality = prefs.getString("mms_image_compression_quality", "NORMAL") ?: "NORMAL",
                swipeGesturesEnabled = prefs.getBoolean("swipe_gestures_enabled", true)
            )
        }
    }

    private val _appSettings: MutableStateFlow<AppSettings> = getOrCreateFlow(prefs)
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    fun setOnboarded(onboarded: Boolean = true) {
        prefs.edit().putBoolean("is_onboarded", onboarded).apply()
        _appSettings.value = _appSettings.value.copy(isOnboarded = onboarded)
    }

    fun setTermsAccepted(accepted: Boolean = true) {
        prefs.edit().putBoolean("has_accepted_terms", accepted).apply()
        _appSettings.value = _appSettings.value.copy(isTermsAccepted = accepted)
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
        _appSettings.value = _appSettings.value.copy(themeMode = mode)
    }

    fun setColorTheme(color: String) {
        prefs.edit().putString("color_theme", color).apply()
        _appSettings.value = _appSettings.value.copy(colorTheme = color)
    }

    fun setSendDelaySeconds(seconds: Int) {
        prefs.edit().putInt("send_delay_seconds", seconds).apply()
        _appSettings.value = _appSettings.value.copy(sendDelaySeconds = seconds)
    }

    fun setDeliveryReports(enabled: Boolean) {
        val mode = if (enabled) "BOTH" else "OFF"
        prefs.edit().putBoolean("delivery_reports", enabled).putString("delivery_report_mode", mode).apply()
        _appSettings.value = _appSettings.value.copy(deliveryReports = enabled, deliveryReportMode = mode)
    }

    fun setDeliveryReportMode(mode: String) {
        val isEnabled = mode != "OFF"
        prefs.edit().putString("delivery_report_mode", mode).putBoolean("delivery_reports", isEnabled).apply()
        _appSettings.value = _appSettings.value.copy(deliveryReportMode = mode, deliveryReports = isEnabled)
    }

    fun setDeliveryReportDuration(duration: String) {
        prefs.edit().putString("delivery_report_duration", duration).apply()
        _appSettings.value = _appSettings.value.copy(deliveryReportDuration = duration)
    }

    fun setSignatureText(sig: String) {
        prefs.edit().putString("signature_text", sig).apply()
        _appSettings.value = _appSettings.value.copy(signatureText = sig)
    }

    fun setBubbleShape(shape: String) {
        prefs.edit().putString("bubble_shape", shape).apply()
        _appSettings.value = _appSettings.value.copy(bubbleShape = shape)
    }

    fun setFontSize(size: String) {
        prefs.edit().putString("font_size", size).apply()
        _appSettings.value = _appSettings.value.copy(fontSize = size)
    }

    fun setFontFamily(font: String) {
        prefs.edit().putString("font_family", font).apply()
        _appSettings.value = _appSettings.value.copy(fontFamily = font)
    }

    fun setVibrateOnSend(enabled: Boolean) {
        prefs.edit().putBoolean("vibrate_on_send", enabled).apply()
        _appSettings.value = _appSettings.value.copy(vibrateOnSend = enabled)
    }

    fun setShowCharacterCounter(enabled: Boolean) {
        prefs.edit().putBoolean("show_char_counter", enabled).apply()
        _appSettings.value = _appSettings.value.copy(showCharacterCounter = enabled)
    }

    fun setSelectedSimSlot(slot: Int) {
        prefs.edit().putInt("selected_sim_slot", slot).apply()
        _appSettings.value = _appSettings.value.copy(selectedSimSlot = slot)
    }

    fun setNotificationSound(enabled: Boolean) {
        prefs.edit().putBoolean("notification_sound", enabled).apply()
        _appSettings.value = _appSettings.value.copy(notificationSound = enabled)
    }

    fun setNotificationVibratePattern(pattern: String) {
        prefs.edit().putString("notification_vibrate_pattern", pattern).apply()
        _appSettings.value = _appSettings.value.copy(notificationVibratePattern = pattern)
    }

    fun setQuickReplyPopup(enabled: Boolean) {
        prefs.edit().putBoolean("quick_reply_popup", enabled).apply()
        _appSettings.value = _appSettings.value.copy(quickReplyPopup = enabled)
    }

    fun setPopupPreviewSize(size: String) {
        prefs.edit().putString("popup_preview_size", size).apply()
        _appSettings.value = _appSettings.value.copy(popupPreviewSize = size)
    }

    fun setAutoRetryAfterTimeout(enabled: Boolean) {
        prefs.edit().putBoolean("auto_retry_after_timeout", enabled).apply()
        _appSettings.value = _appSettings.value.copy(autoRetryAfterTimeout = enabled)
    }

    fun setRepeatNotificationCount(count: Int) {
        prefs.edit().putInt("repeat_notification_count", count).apply()
        _appSettings.value = _appSettings.value.copy(repeatNotificationCount = count)
    }

    fun setUserName(name: String) {
        prefs.edit().putString("user_name", name).apply()
        _appSettings.value = _appSettings.value.copy(userName = name)
    }

    fun setUserPhoneNumber(phone: String) {
        prefs.edit().putString("user_phone_number", phone).apply()
        _appSettings.value = _appSettings.value.copy(userPhoneNumber = phone)
    }

    fun setUserAvatarColor(colorHex: String) {
        prefs.edit().putString("user_avatar_color", colorHex).apply()
        _appSettings.value = _appSettings.value.copy(userAvatarColor = colorHex)
    }

    fun setUserStatus(status: String) {
        prefs.edit().putString("user_status", status).apply()
        _appSettings.value = _appSettings.value.copy(userStatus = status)
    }

    fun setLastSmsSyncTimestamp(timestamp: Long) {
        prefs.edit().putLong("last_sms_sync_timestamp", timestamp).apply()
        _appSettings.value = _appSettings.value.copy(lastSmsSyncTimestamp = timestamp)
    }

    fun setDefaultChatWallpaper(wallpaper: String) {
        prefs.edit().putString("default_chat_wallpaper", wallpaper).apply()
        _appSettings.value = _appSettings.value.copy(defaultChatWallpaper = wallpaper)
    }

    fun setMmsImageCompressionQuality(quality: String) {
        prefs.edit().putString("mms_image_compression_quality", quality).apply()
        _appSettings.value = _appSettings.value.copy(mmsImageCompressionQuality = quality)
    }

    fun setSwipeGesturesEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("swipe_gestures_enabled", enabled).apply()
        _appSettings.value = _appSettings.value.copy(swipeGesturesEnabled = enabled)
    }

    fun isConversationMuted(conversationId: String): Boolean {
        val set = prefs.getStringSet("muted_conversations", emptySet()) ?: emptySet()
        return set.contains(conversationId)
    }

    fun setConversationMuted(conversationId: String, muted: Boolean) {
        val currentSet = prefs.getStringSet("muted_conversations", emptySet())?.toMutableSet() ?: mutableSetOf()
        if (muted) currentSet.add(conversationId) else currentSet.remove(conversationId)
        prefs.edit().putStringSet("muted_conversations", currentSet).apply()
    }

    fun getMutedConversations(): Set<String> {
        return prefs.getStringSet("muted_conversations", emptySet()) ?: emptySet()
    }

    fun getPreferredSimForConversation(conversationId: String): Int? {
        if (!prefs.contains("preferred_sim_$conversationId")) return null
        return prefs.getInt("preferred_sim_$conversationId", 0)
    }

    fun setPreferredSimForConversation(conversationId: String, simSlot: Int) {
        prefs.edit().putInt("preferred_sim_$conversationId", simSlot).apply()
    }
}
