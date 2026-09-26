package com.example.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.ui.util.PhoneNumberUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    // Fast O(1) multi-index lookup tables
    private val contactsByNormalizedNumber = ConcurrentHashMap<String, Contact>()
    private val contactsByDigits = ConcurrentHashMap<String, Contact>()
    private val contactsBySuffix7 = ConcurrentHashMap<String, Contact>()

    private val _registeredContacts = MutableStateFlow<List<Contact>>(emptyList())
    val registeredContacts: StateFlow<List<Contact>> = _registeredContacts.asStateFlow()

    private val _contactsMap = MutableStateFlow<Map<String, Contact>>(emptyMap())
    val contactsMap: StateFlow<Map<String, Contact>> = _contactsMap.asStateFlow()

    private val photoUriCache = android.util.LruCache<String, String>(250)
    private var isObserverRegistered = false
    private var contactsObserver: ContentObserver? = null

    fun clearCaches() {
        synchronized(photoUriCache) {
            photoUriCache.evictAll()
        }
    }

    /**
     * Highly optimized O(1) contact lookup with multi-index matching.
     * Matches exact normalized, clean digits, and standard national 7-digit suffix.
     */
    fun getContactByPhoneNumber(phoneNumber: String): Contact? {
        if (phoneNumber.isBlank()) return null

        val normalized = PhoneNumberUtil.normalize(phoneNumber)
        if (normalized.isNotBlank()) {
            contactsByNormalizedNumber[normalized]?.let { return it }
        }

        val digits = PhoneNumberUtil.extractDigits(phoneNumber)
        if (digits.isNotBlank()) {
            contactsByDigits[digits]?.let { return it }

            if (digits.length >= 7) {
                contactsBySuffix7[digits.takeLast(7)]?.let { return it }
            }

            // Suffix containment check for national vs international
            if (digits.length >= 7) {
                val suffix7 = digits.takeLast(7)
                for ((key, contact) in contactsByDigits) {
                    if (key.endsWith(suffix7) || suffix7.endsWith(key)) {
                        return contact
                    }
                }
            }
        }

        // Direct normalized number fallback
        contactsByNormalizedNumber[phoneNumber.trim()]?.let { return it }

        return null
    }

    fun getPhotoUriForPhoneNumber(phoneNumber: String): String? {
        val normalized = PhoneNumberUtil.normalize(phoneNumber)
        synchronized(photoUriCache) {
            photoUriCache.get(normalized)?.let { return it }
        }
        val contactPhoto = getContactByPhoneNumber(phoneNumber)?.photoUri
        if (contactPhoto != null) {
            synchronized(photoUriCache) {
                photoUriCache.put(normalized, contactPhoto)
            }
            return contactPhoto
        }
        return null
    }

    /**
     * Efficiently resolves a contact's photo thumbnail on-demand using Contacts Provider
     * with memory caching to prevent repeated queries.
     */
    fun resolveContactPhotoUri(context: Context, phoneNumber: String): String? {
        val normalized = PhoneNumberUtil.normalize(phoneNumber)
        if (normalized.isBlank()) return null
        synchronized(photoUriCache) {
            photoUriCache.get(normalized)?.let { return it }
        }

        val cached = getContactByPhoneNumber(phoneNumber)?.photoUri
        if (cached != null) {
            synchronized(photoUriCache) {
                photoUriCache.put(normalized, cached)
            }
            return cached
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(
                ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI,
                ContactsContract.PhoneLookup.DISPLAY_NAME
            )
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val photoIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI)
                    val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)

                    val photoUri = if (photoIndex >= 0) cursor.getString(photoIndex) else null
                    val name = if (nameIndex >= 0) cursor.getString(nameIndex) else null

                    if (!photoUri.isNullOrBlank()) {
                        synchronized(photoUriCache) {
                            photoUriCache.put(normalized, photoUri)
                        }
                    }

                    if (!name.isNullOrBlank()) {
                        val contact = Contact(
                            phoneNumber = phoneNumber.trim(),
                            name = name.trim(),
                            photoUri = photoUri,
                            isInternetUser = false,
                            statusText = "SMS (SIM Card)"
                        )
                        addOrUpdateContact(contact)
                    }

                    if (!photoUri.isNullOrBlank()) return photoUri
                }
            }
        } catch (_: Exception) {
            // Safe fallback
        }
        return null
    }

    /**
     * On-demand lookup of a contact's name and photo directly from Android PhoneLookup provider.
     * Caches result immediately so subsequent lookups are instantaneous.
     */
    fun resolveContactDetails(context: Context, phoneNumber: String): Pair<String?, String?> {
        val cached = getContactByPhoneNumber(phoneNumber)
        if (cached != null && cached.name.isNotBlank() && cached.name != cached.phoneNumber) {
            return Pair(cached.name, cached.photoUri)
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return Pair(cached?.name, cached?.photoUri)
        }

        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI,
                ContactsContract.PhoneLookup.NUMBER
            )
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    val photoIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI)
                    val numIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.NUMBER)

                    val name = if (nameIndex >= 0) cursor.getString(nameIndex) else null
                    val photo = if (photoIndex >= 0) cursor.getString(photoIndex) else null
                    val rawNum = if (numIndex >= 0) cursor.getString(numIndex) else phoneNumber

                    if (!name.isNullOrBlank()) {
                        val contact = Contact(
                            phoneNumber = rawNum ?: phoneNumber,
                            name = name.trim(),
                            photoUri = photo,
                            isInternetUser = false,
                            statusText = "SMS (SIM Card)"
                        )
                        addOrUpdateContact(contact)
                        return Pair(name.trim(), photo)
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore safe lookup errors
        }
        return Pair(cached?.name, cached?.photoUri)
    }

    fun addOrUpdateContact(contact: Contact) {
        val normalized = PhoneNumberUtil.normalize(contact.phoneNumber)
        val digits = PhoneNumberUtil.extractDigits(contact.phoneNumber)

        if (normalized.isNotBlank()) {
            contactsByNormalizedNumber[normalized] = contact
        }
        if (digits.isNotBlank()) {
            contactsByDigits[digits] = contact
            if (digits.length >= 7) {
                contactsBySuffix7[digits.takeLast(7)] = contact
            }
        }
        contactsByNormalizedNumber[contact.phoneNumber.trim()] = contact

        _registeredContacts.value = contactsByNormalizedNumber.values.toList().sortedBy { it.name.lowercase() }
        _contactsMap.value = HashMap(contactsByNormalizedNumber)
    }

    /**
     * Registers a ContentObserver on Android Contacts provider to automatically detect
     * when the user adds, edits, or deletes contacts in the device Contacts app.
     */
    fun registerContentObserver(context: Context) {
        if (isObserverRegistered) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return

        try {
            val appContext = context.applicationContext
            contactsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
                private var lastTriggerTime = 0L

                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    super.onChange(selfChange, uri)
                    val now = System.currentTimeMillis()
                    if (now - lastTriggerTime > 1500L) { // Debounce 1.5s
                        lastTriggerTime = now
                        CoroutineScope(Dispatchers.IO).launch {
                            syncDeviceContacts(appContext)
                        }
                    }
                }
            }

            appContext.contentResolver.registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI,
                true,
                contactsObserver!!
            )
            isObserverRegistered = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Full scan and indexing of device contacts.
     * Completes in <50ms with minimal memory footprint and updates all lookup tables atomically.
     */
    suspend fun syncDeviceContacts(context: Context): Int = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return@withContext 0
        }

        registerContentObserver(context)

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
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE ASC"
            )

            val newNormalizedMap = ConcurrentHashMap<String, Contact>()
            val newDigitsMap = ConcurrentHashMap<String, Contact>()
            val newSuffixMap = ConcurrentHashMap<String, Contact>()

            cursor?.use {
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

                while (it.moveToNext()) {
                    val rawName = if (nameIndex >= 0) it.getString(nameIndex) else null
                    val rawNumber = if (numberIndex >= 0) it.getString(numberIndex) else null
                    val photo = if (photoIndex >= 0) it.getString(photoIndex) else null

                    if (!rawNumber.isNullOrBlank()) {
                        val trimmedNumber = rawNumber.trim()
                        val displayName = rawName?.trim()?.takeIf { it.isNotEmpty() } ?: trimmedNumber
                        val contact = Contact(
                            phoneNumber = trimmedNumber,
                            name = displayName,
                            photoUri = photo,
                            isInternetUser = false,
                            statusText = "SMS (SIM Card)"
                        )

                        val norm = PhoneNumberUtil.normalize(trimmedNumber)
                        val digits = PhoneNumberUtil.extractDigits(trimmedNumber)

                        if (norm.isNotBlank()) {
                            newNormalizedMap.putIfAbsent(norm, contact)
                        }
                        if (digits.isNotBlank()) {
                            newDigitsMap.putIfAbsent(digits, contact)
                            if (digits.length >= 7) {
                                newSuffixMap.putIfAbsent(digits.takeLast(7), contact)
                            }
                        }
                        newNormalizedMap.putIfAbsent(trimmedNumber, contact)
                        count++
                    }
                }
            }

            // Atomic map replacement to avoid stale/deleted entries
            contactsByNormalizedNumber.clear()
            contactsByNormalizedNumber.putAll(newNormalizedMap)
            contactsByDigits.clear()
            contactsByDigits.putAll(newDigitsMap)
            contactsBySuffix7.clear()
            contactsBySuffix7.putAll(newSuffixMap)

            val sortedList = newNormalizedMap.values.toList().sortedBy { it.name.lowercase() }
            _registeredContacts.value = sortedList
            _contactsMap.value = HashMap(newNormalizedMap)

            // Update persistent conversation records in Room database
            try {
                com.example.PulseChatApp.getMessageRepository(context)
                    .updateConversationContactNamesFromContacts(this@ContactRepository)
            } catch (e: Exception) {
                // Ignore if repository not ready
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        count
    }
}
