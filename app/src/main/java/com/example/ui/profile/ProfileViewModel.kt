package com.example.ui.profile

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.BlockedContactEntity
import com.example.data.local.entity.ScheduledMessageEntity
import com.example.data.preference.AppSettings
import com.example.data.preference.UserPreferences
import com.example.data.repository.ContactRepository
import com.example.data.repository.MessageRepository
import com.example.ui.home.checkAllSmsPermissions
import com.example.ui.util.DefaultSmsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userPreferences: UserPreferences,
    private val messageRepository: MessageRepository,
    private val contactRepository: ContactRepository
) : ViewModel() {

    val appSettings: StateFlow<AppSettings> = userPreferences.appSettings

    val blockedContacts: StateFlow<List<BlockedContactEntity>> =
        messageRepository.getAllBlockedContacts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allScheduledMessages: StateFlow<List<ScheduledMessageEntity>> =
        messageRepository.getAllScheduledMessages()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val registeredContacts = contactRepository.registeredContacts

    private val _isDefaultSmsApp = MutableStateFlow(false)
    val isDefaultSmsApp: StateFlow<Boolean> = _isDefaultSmsApp.asStateFlow()

    private val _isBatteryOptimized = MutableStateFlow(false)
    val isBatteryOptimized: StateFlow<Boolean> = _isBatteryOptimized.asStateFlow()

    private val _hasSmsPermission = MutableStateFlow(false)
    val hasSmsPermission: StateFlow<Boolean> = _hasSmsPermission.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    fun checkStatus(context: Context) {
        _isDefaultSmsApp.value = DefaultSmsHelper.isDefaultSmsApp(context)
        _isBatteryOptimized.value = !DefaultSmsHelper.isIgnoringBatteryOptimizations(context)
        _hasSmsPermission.value = checkAllSmsPermissions(context)
    }

    fun createDefaultSmsIntent(context: Context): Intent? {
        return DefaultSmsHelper.createDefaultSmsIntent(context)
    }

    fun createRequestBatteryOptimizationIntent(context: Context): Intent? {
        return DefaultSmsHelper.createRequestBatteryOptimizationIntent(context)
    }

    fun createAppSettingsIntent(context: Context): Intent {
        return DefaultSmsHelper.createAppSettingsIntent(context)
    }

    fun syncDeviceData(context: Context) {
        viewModelScope.launch {
            contactRepository.syncDeviceContacts(context)
            val smsCount = messageRepository.syncDeviceSms()
            _syncMessage.value = "Synced SIM messages and contacts ($smsCount messages imported)."
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    fun setThemeMode(mode: String) {
        userPreferences.setThemeMode(mode)
    }

    fun setColorTheme(color: String) {
        userPreferences.setColorTheme(color)
    }

    fun setSendDelaySeconds(seconds: Int) {
        userPreferences.setSendDelaySeconds(seconds)
    }

    fun setDeliveryReports(enabled: Boolean) {
        userPreferences.setDeliveryReports(enabled)
    }

    fun setDeliveryReportMode(mode: String) {
        userPreferences.setDeliveryReportMode(mode)
    }

    fun setSignatureText(sig: String) {
        userPreferences.setSignatureText(sig)
    }

    fun setBubbleShape(shape: String) {
        userPreferences.setBubbleShape(shape)
    }

    fun setFontSize(size: String) {
        userPreferences.setFontSize(size)
    }

    fun setFontFamily(font: String) {
        userPreferences.setFontFamily(font)
    }

    fun setVibrateOnSend(enabled: Boolean) {
        userPreferences.setVibrateOnSend(enabled)
    }

    fun setShowCharacterCounter(enabled: Boolean) {
        userPreferences.setShowCharacterCounter(enabled)
    }

    fun setMmsSizeLimit(limit: String) {
        userPreferences.setMmsSizeLimit(limit)
    }

    fun setMmsImageCompressionQuality(quality: String) {
        userPreferences.setMmsImageCompressionQuality(quality)
    }

    fun setAutoDownloadMms(mode: String) {
        userPreferences.setAutoDownloadMms(mode)
    }

    fun setAutoSavePhotos(enabled: Boolean) {
        userPreferences.setAutoSavePhotos(enabled)
    }

    fun setNotificationSound(enabled: Boolean) {
        userPreferences.setNotificationSound(enabled)
    }

    fun setNotificationVibratePattern(pattern: String) {
        userPreferences.setNotificationVibratePattern(pattern)
    }

    fun setQuickReplyPopup(enabled: Boolean) {
        userPreferences.setQuickReplyPopup(enabled)
    }

    fun setPopupPreviewSize(size: String) {
        userPreferences.setPopupPreviewSize(size)
    }

    fun setAutoRetryAfterTimeout(enabled: Boolean) {
        userPreferences.setAutoRetryAfterTimeout(enabled)
    }

    fun setRepeatNotificationCount(count: Int) {
        userPreferences.setRepeatNotificationCount(count)
    }

    fun setUserName(name: String) {
        userPreferences.setUserName(name)
    }

    fun setUserPhoneNumber(phone: String) {
        userPreferences.setUserPhoneNumber(phone)
    }

    fun setUserAvatarColor(colorHex: String) {
        userPreferences.setUserAvatarColor(colorHex)
    }

    fun setUserStatus(status: String) {
        userPreferences.setUserStatus(status)
    }

    fun unblockContact(phoneNumber: String) {
        viewModelScope.launch {
            messageRepository.unblockContact(phoneNumber)
        }
    }

    fun cancelScheduledMessage(id: Long) {
        viewModelScope.launch {
            messageRepository.cancelScheduledMessage(id)
        }
    }

    fun sendScheduledMessageNow(scheduled: ScheduledMessageEntity) {
        viewModelScope.launch {
            messageRepository.sendScheduledMessageNow(
                id = scheduled.id,
                recipientPhone = scheduled.recipientPhoneNumber,
                recipientName = scheduled.recipientName,
                content = scheduled.content
            )
        }
    }
}
