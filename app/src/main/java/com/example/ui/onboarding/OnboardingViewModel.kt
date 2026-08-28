package com.example.ui.onboarding

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preference.UserPreferences
import com.example.data.repository.MessageRepository
import com.example.ui.util.AvatarUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val userPreferences: UserPreferences,
    private val messageRepository: MessageRepository
) : ViewModel() {

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    private val _displayName = MutableStateFlow("")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _selectedAvatar = MutableStateFlow("avatar_1")
    val selectedAvatar: StateFlow<String> = _selectedAvatar.asStateFlow()

    private val _isDefaultSmsApp = MutableStateFlow(false)
    val isDefaultSmsApp: StateFlow<Boolean> = _isDefaultSmsApp.asStateFlow()

    fun updatePhoneNumber(input: String) {
        _phoneNumber.value = input
    }

    fun updateDisplayName(input: String) {
        _displayName.value = input
    }

    fun selectAvatar(avatarId: String) {
        _selectedAvatar.value = avatarId
    }

    fun checkDefaultSmsStatus(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            _isDefaultSmsApp.value = roleManager?.isRoleHeld(RoleManager.ROLE_SMS) == true
        } else {
            val defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(context)
            _isDefaultSmsApp.value = defaultSmsPackage == context.packageName
        }
    }

    fun createDefaultSmsIntent(context: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.createRequestRoleIntent(RoleManager.ROLE_SMS)
        } else {
            Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
            }
        }
    }

    fun completeOnboarding(onSuccess: () -> Unit) {
        val phone = _phoneNumber.value.trim()
        val name = _displayName.value.trim().ifBlank { "User" }
        val avatar = _selectedAvatar.value

        userPreferences.saveProfile(phone, name, avatar)
        onSuccess()
    }
}
