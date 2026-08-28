package com.example.ui.profile

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preference.UserPreferences
import com.example.data.repository.ContactRepository
import com.example.data.repository.MessageRepository
import com.example.ui.home.checkAllSmsPermissions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userPreferences: UserPreferences,
    private val messageRepository: MessageRepository,
    private val contactRepository: ContactRepository
) : ViewModel() {

    val userProfile = userPreferences.userProfile

    private val _isDefaultSmsApp = MutableStateFlow(false)
    val isDefaultSmsApp: StateFlow<Boolean> = _isDefaultSmsApp.asStateFlow()

    private val _hasSmsPermission = MutableStateFlow(false)
    val hasSmsPermission: StateFlow<Boolean> = _hasSmsPermission.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    fun checkStatus(context: Context) {
        checkDefaultSmsStatus(context)
        _hasSmsPermission.value = checkAllSmsPermissions(context)
    }

    private fun checkDefaultSmsStatus(context: Context) {
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

    fun syncDeviceData(context: Context) {
        viewModelScope.launch {
            val contactsCount = contactRepository.syncDeviceContacts(context)
            val smsCount = messageRepository.syncDeviceSms(userProfile.value.phoneNumber)
            _syncMessage.value = "Synced $contactsCount contacts and $smsCount text messages."
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    fun updateProfile(name: String, phone: String, avatar: String) {
        userPreferences.saveProfile(phone, name, avatar)
    }
}
