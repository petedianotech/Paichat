package com.example.ui.newchat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.Contact
import com.example.data.repository.ContactRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NewChatViewModel(
    private val contactRepository: ContactRepository
) : ViewModel() {

    private val _phoneSearchQuery = MutableStateFlow("")
    val phoneSearchQuery: StateFlow<String> = _phoneSearchQuery.asStateFlow()

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
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateSearchQuery(query: String) {
        _phoneSearchQuery.value = query
    }

    fun syncContacts(context: Context) {
        viewModelScope.launch {
            contactRepository.syncDeviceContacts(context)
        }
    }

    fun isSearchNumberPulseChatUser(phone: String): Boolean {
        return contactRepository.isContactInternetUser(phone)
    }

    fun addCustomContact(phone: String, isInternet: Boolean): Contact {
        val contact = Contact(
            phoneNumber = phone,
            name = phone,
            avatarId = "avatar_1",
            isInternetUser = isInternet,
            statusText = if (isInternet) "Internet Chat (Online)" else "Regular Text (SMS)"
        )
        contactRepository.addOrUpdateContact(contact)
        return contact
    }
}
