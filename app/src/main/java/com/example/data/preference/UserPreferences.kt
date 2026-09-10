package com.example.data.preference

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val isOnboarded: Boolean = false,
    val themeMode: String = "SYSTEM", // SYSTEM, LIGHT, DARK, AMOLED
    val colorTheme: String = "BLUE", // BLUE, INDIGO, PURPLE, ROSE, TEAL, AMBER
    val sendDelaySeconds: Int = 3, // 0 (off), 1, 2, 3, 5, 10
    val deliveryReports: Boolean = true,
    val signatureText: String = "",
    val bubbleShape: String = "ROUNDED", // ROUNDED, PILL, SQUARE
    val fontSize: String = "NORMAL", // SMALL, NORMAL, LARGE, EXTRA_LARGE
    val vibrateOnSend: Boolean = true,
    val showCharacterCounter: Boolean = true,
    val selectedSimSlot: Int = 0, // 0 = SIM 1, 1 = SIM 2
    // Textra-grade MMS & Media settings
    val mmsSizeLimit: String = "1MB", // 300KB, 600KB, 1MB, 2MB
    val autoDownloadMms: String = "ALWAYS", // ALWAYS, WIFI_ONLY, NEVER
    val autoSavePhotos: Boolean = false,
    // Notification & Quick Reply settings
    val notificationSound: Boolean = true,
    val notificationVibratePattern: String = "NORMAL", // NORMAL, SHORT, LONG, OFF
    val quickReplyPopup: Boolean = true,
    val repeatNotificationCount: Int = 0 // 0 = Never, 1, 2, 5
)

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("sms_app_prefs", Context.MODE_PRIVATE)

    private val _appSettings = MutableStateFlow(loadSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    private fun loadSettings(): AppSettings {
        val onboarded = prefs.getBoolean("is_onboarded", false)
        val theme = prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM"
        val color = prefs.getString("color_theme", "BLUE") ?: "BLUE"
        val delay = prefs.getInt("send_delay_seconds", 3)
        val delivery = prefs.getBoolean("delivery_reports", true)
        val sig = prefs.getString("signature_text", "") ?: ""
        val shape = prefs.getString("bubble_shape", "ROUNDED") ?: "ROUNDED"
        val font = prefs.getString("font_size", "NORMAL") ?: "NORMAL"
        val vibrate = prefs.getBoolean("vibrate_on_send", true)
        val charCounter = prefs.getBoolean("show_char_counter", true)
        val simSlot = prefs.getInt("selected_sim_slot", 0)

        val mmsSize = prefs.getString("mms_size_limit", "1MB") ?: "1MB"
        val autoMms = prefs.getString("auto_download_mms", "ALWAYS") ?: "ALWAYS"
        val autoSave = prefs.getBoolean("auto_save_photos", false)
        val notifSound = prefs.getBoolean("notification_sound", true)
        val notifVib = prefs.getString("notification_vibrate_pattern", "NORMAL") ?: "NORMAL"
        val quickReply = prefs.getBoolean("quick_reply_popup", true)
        val repeatNotif = prefs.getInt("repeat_notification_count", 0)

        return AppSettings(
            isOnboarded = onboarded,
            themeMode = theme,
            colorTheme = color,
            sendDelaySeconds = delay,
            deliveryReports = delivery,
            signatureText = sig,
            bubbleShape = shape,
            fontSize = font,
            vibrateOnSend = vibrate,
            showCharacterCounter = charCounter,
            selectedSimSlot = simSlot,
            mmsSizeLimit = mmsSize,
            autoDownloadMms = autoMms,
            autoSavePhotos = autoSave,
            notificationSound = notifSound,
            notificationVibratePattern = notifVib,
            quickReplyPopup = quickReply,
            repeatNotificationCount = repeatNotif
        )
    }

    fun setOnboarded(onboarded: Boolean = true) {
        prefs.edit().putBoolean("is_onboarded", onboarded).apply()
        _appSettings.value = _appSettings.value.copy(isOnboarded = onboarded)
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
        prefs.edit().putBoolean("delivery_reports", enabled).apply()
        _appSettings.value = _appSettings.value.copy(deliveryReports = enabled)
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

    fun setMmsSizeLimit(limit: String) {
        prefs.edit().putString("mms_size_limit", limit).apply()
        _appSettings.value = _appSettings.value.copy(mmsSizeLimit = limit)
    }

    fun setAutoDownloadMms(mode: String) {
        prefs.edit().putString("auto_download_mms", mode).apply()
        _appSettings.value = _appSettings.value.copy(autoDownloadMms = mode)
    }

    fun setAutoSavePhotos(enabled: Boolean) {
        prefs.edit().putBoolean("auto_save_photos", enabled).apply()
        _appSettings.value = _appSettings.value.copy(autoSavePhotos = enabled)
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

    fun setRepeatNotificationCount(count: Int) {
        prefs.edit().putInt("repeat_notification_count", count).apply()
        _appSettings.value = _appSettings.value.copy(repeatNotificationCount = count)
    }
}
