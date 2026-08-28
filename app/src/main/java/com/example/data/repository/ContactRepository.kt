package com.example.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Contact(
    val phoneNumber: String,
    val name: String,
    val avatarId: String,
    val isInternetUser: Boolean,
    val statusText: String = if (isInternetUser) "Internet Chat (Online)" else "Regular Text (SMS)"
)

class ContactRepository {

    // Real contacts loaded from phone and conversations
    private val _registeredContacts = MutableStateFlow<List<Contact>>(emptyList())
    val registeredContacts: StateFlow<List<Contact>> = _registeredContacts.asStateFlow()

    fun isContactInternetUser(phoneNumber: String): Boolean {
        val normalized = normalizePhoneNumber(phoneNumber)
        return _registeredContacts.value.find { normalizePhoneNumber(it.phoneNumber) == normalized }?.isInternetUser ?: false
    }

    fun getContactByPhoneNumber(phoneNumber: String): Contact? {
        val normalized = normalizePhoneNumber(phoneNumber)
        return _registeredContacts.value.find { normalizePhoneNumber(it.phoneNumber) == normalized }
    }

    fun addOrUpdateContact(contact: Contact) {
        val current = _registeredContacts.value.toMutableList()
        val index = current.indexOfFirst { normalizePhoneNumber(it.phoneNumber) == normalizePhoneNumber(contact.phoneNumber) }
        if (index >= 0) {
            current[index] = contact
        } else {
            current.add(contact)
        }
        _registeredContacts.value = current
    }

    fun syncDeviceContacts(context: Context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )

            cursor?.use {
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                val updatedList = _registeredContacts.value.toMutableList()
                var avatarCounter = 1

                while (it.moveToNext()) {
                    val name = if (nameIndex >= 0) it.getString(nameIndex) else "Contact"
                    val number = if (numberIndex >= 0) it.getString(numberIndex) else ""

                    if (number.isNotBlank()) {
                        val norm = normalizePhoneNumber(number)
                        val existingIndex = updatedList.indexOfFirst { c -> normalizePhoneNumber(c.phoneNumber) == norm }
                        if (existingIndex < 0) {
                            val avatarId = "avatar_${(avatarCounter % 5) + 1}"
                            avatarCounter++
                            updatedList.add(
                                Contact(
                                    phoneNumber = number,
                                    name = name ?: number,
                                    avatarId = avatarId,
                                    isInternetUser = false,
                                    statusText = "Regular Text (SMS)"
                                )
                            )
                        }
                    }
                }
                _registeredContacts.value = updatedList
            }
        } catch (_: Exception) {
            // Handled safely
        }
    }

    private fun normalizePhoneNumber(phone: String): String {
        return phone.replace(Regex("[^0-9+]"), "")
    }
}
