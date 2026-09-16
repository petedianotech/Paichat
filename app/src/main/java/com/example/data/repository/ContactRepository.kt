package com.example.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

data class Contact(
    val phoneNumber: String,
    val name: String,
    val photoUri: String? = null,
    val isInternetUser: Boolean = false,
    val statusText: String = "SMS (SIM Card)"
)

class ContactRepository {

    private val contactsByNormalizedNumber = ConcurrentHashMap<String, Contact>()
    private val _registeredContacts = MutableStateFlow<List<Contact>>(emptyList())
    val registeredContacts: StateFlow<List<Contact>> = _registeredContacts.asStateFlow()

    private val _contactsMap = MutableStateFlow<Map<String, Contact>>(emptyMap())
    val contactsMap: StateFlow<Map<String, Contact>> = _contactsMap.asStateFlow()

    fun getContactByPhoneNumber(phoneNumber: String): Contact? {
        val normalized = normalizePhoneNumber(phoneNumber)
        if (normalized.isBlank()) return null
        
        // Exact normalized match
        contactsByNormalizedNumber[normalized]?.let { return it }

        // Last 7-10 digits matching for international vs local dialing compatibility
        if (normalized.length >= 7) {
            val suffix = normalized.takeLast(7)
            for ((key, contact) in contactsByNormalizedNumber) {
                if (key.endsWith(suffix)) return contact
            }
        }
        return null
    }

    private val photoUriCache = ConcurrentHashMap<String, String>()

    fun getPhotoUriForPhoneNumber(phoneNumber: String): String? {
        val normalized = normalizePhoneNumber(phoneNumber)
        photoUriCache[normalized]?.let { return it }
        val contactPhoto = getContactByPhoneNumber(phoneNumber)?.photoUri
        if (contactPhoto != null) {
            photoUriCache[normalized] = contactPhoto
            return contactPhoto
        }
        return null
    }

    /**
     * Efficiently resolves a contact's photo thumbnail on-demand using Contacts Provider
     * with memory caching to prevent repeated queries.
     */
    fun resolveContactPhotoUri(context: Context, phoneNumber: String): String? {
        val normalized = normalizePhoneNumber(phoneNumber)
        if (normalized.isBlank()) return null
        photoUriCache[normalized]?.let { return it }

        val cached = getContactByPhoneNumber(phoneNumber)?.photoUri
        if (cached != null) {
            photoUriCache[normalized] = cached
            return cached
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        try {
            val uri = android.net.Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                android.net.Uri.encode(phoneNumber)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val photoIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI)
                    if (photoIndex >= 0) {
                        val photoUri = cursor.getString(photoIndex)
                        if (!photoUri.isNullOrBlank()) {
                            photoUriCache[normalized] = photoUri
                            return photoUri
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore safe lookup errors
        }
        return null
    }

    fun addOrUpdateContact(contact: Contact) {
        val normalized = normalizePhoneNumber(contact.phoneNumber)
        if (normalized.isNotBlank()) {
            contactsByNormalizedNumber[normalized] = contact
            _registeredContacts.value = contactsByNormalizedNumber.values.sortedBy { it.name }
            _contactsMap.value = contactsByNormalizedNumber.toMap()
        }
    }

    suspend fun syncDeviceContacts(context: Context): Int = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return@withContext 0
        }

        var count = 0
        try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
            )

            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            val tempMap = LinkedHashMap<String, Contact>()

            cursor?.use {
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

                while (it.moveToNext()) {
                    val name = if (nameIndex >= 0) it.getString(nameIndex) else "Contact"
                    val number = if (numberIndex >= 0) it.getString(numberIndex) else ""
                    val photo = if (photoIndex >= 0) it.getString(photoIndex) else null

                    if (!number.isNullOrBlank()) {
                        val norm = normalizePhoneNumber(number)
                        if (norm.isNotBlank() && !tempMap.containsKey(norm)) {
                            val contact = Contact(
                                phoneNumber = number.trim(),
                                name = name?.trim() ?: number.trim(),
                                photoUri = photo,
                                isInternetUser = false,
                                statusText = "SMS (SIM Card)"
                            )
                            tempMap[norm] = contact
                            count++
                        }
                    }
                }
            }

            contactsByNormalizedNumber.putAll(tempMap)
            _registeredContacts.value = contactsByNormalizedNumber.values.sortedBy { it.name }
            _contactsMap.value = contactsByNormalizedNumber.toMap()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        count
    }

    private fun normalizePhoneNumber(phone: String): String {
        return com.example.ui.util.PhoneNumberUtil.normalize(phone)
    }
}
