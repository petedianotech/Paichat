package com.example.ui.newchat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.Contact
import com.example.data.repository.ContactRepository
import com.example.data.repository.MessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NewChatViewModel(
    private val contactRepository: ContactRepository,
    private val messageRepository: MessageRepository
) : ViewModel() {

    private val _phoneSearchQuery = MutableStateFlow("")
    val phoneSearchQuery: StateFlow<String> = _phoneSearchQuery.asStateFlow()

    private val _selectedRecipients = MutableStateFlow<List<Contact>>(emptyList())
    val selectedRecipients: StateFlow<List<Contact>> = _selectedRecipients.asStateFlow()

    val contactsList: StateFlow<List<Contact>> = combine(
        contactRepository.registeredContacts,
        _phoneSearchQuery
    ) { contacts, query ->
        if (query.isBlank()) {
            contacts
        } else {
            contacts.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.phoneNumber.contains(query)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateSearchQuery(query: String) {
        _phoneSearchQuery.value = query
    }

    fun toggleRecipientSelection(contact: Contact) {
        val current = _selectedRecipients.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.phoneNumber == contact.phoneNumber }
        if (existingIndex >= 0) {
            current.removeAt(existingIndex)
        } else {
            current.add(contact)
        }
        _selectedRecipients.value = current
    }

    fun removeRecipient(phoneNumber: String) {
        _selectedRecipients.value = _selectedRecipients.value.filter { it.phoneNumber != phoneNumber }
    }

    fun syncContacts(context: Context) {
        viewModelScope.launch {
            contactRepository.syncDeviceContacts(context)
        }
    }

    fun addCustomContact(phone: String): Contact {
        val cleanPhone = phone.trim()
        val contact = Contact(
            phoneNumber = cleanPhone,
            name = cleanPhone,
            isInternetUser = false,
            statusText = "SMS (SIM Card)"
        )
        contactRepository.addOrUpdateContact(contact)
        return contact
    }

    fun sendGroupSms(messageText: String, onComplete: () -> Unit) {
        val recipients = _selectedRecipients.value.map { it.phoneNumber }
        if (recipients.isEmpty() || messageText.isBlank()) return

        viewModelScope.launch {
            messageRepository.sendBroadcastOrGroupSms(
                recipients = recipients,
                content = messageText.trim()
            )
            onComplete()
        }
    }
}
