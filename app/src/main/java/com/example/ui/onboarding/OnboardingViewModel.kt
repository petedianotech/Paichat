package com.example.ui.onboarding

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.os.Build
import android.provider.Telephony
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.SyncProgress
import com.example.data.preference.UserPreferences
import com.example.data.repository.ContactRepository
import com.example.data.repository.MessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val userPreferences: UserPreferences,
    private val messageRepository: MessageRepository,
    private val contactRepository: ContactRepository
) : ViewModel() {

    val syncProgress: StateFlow<SyncProgress> = messageRepository.syncProgress

    private val _isDefaultSmsApp = MutableStateFlow(false)
    val isDefaultSmsApp: StateFlow<Boolean> = _isDefaultSmsApp.asStateFlow()

    private val _hasPermissions = MutableStateFlow(false)
    val hasPermissions: StateFlow<Boolean> = _hasPermissions.asStateFlow()

    fun checkStatus(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            _isDefaultSmsApp.value = roleManager?.isRoleHeld(RoleManager.ROLE_SMS) == true
        } else {
            val defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(context)
            _isDefaultSmsApp.value = defaultSmsPackage == context.packageName
        }

        val hasSms = ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        val hasContacts = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        _hasPermissions.value = hasSms && hasContacts
    }

    fun createDefaultSmsIntent(context: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.createRequestRoleIntent(RoleManager.ROLE_SMS)
        } else {
            @Suppress("DEPRECATION")
            Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
            }
        }
    }

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun completeOnboarding(context: Context, onComplete: () -> Unit) {
        _isSyncing.value = true
        viewModelScope.launch {
            try {
                launch {
                    contactRepository.syncDeviceContacts(context)
                }
                messageRepository.syncDeviceSms()
            } catch (exception: Exception) {
                Log.e("OnboardingViewModel", "Initial SMS sync failed", exception)
            }
            userPreferences.setOnboarded(true)
            _isSyncing.value = false
            onComplete()
        }
    }
}
