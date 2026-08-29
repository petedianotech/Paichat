package com.example.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.ConversationEntity
import com.example.data.preference.UserPreferences
import com.example.data.repository.ContactRepository
import com.example.data.repository.MessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val messageRepository: MessageRepository,
    private val contactRepository: ContactRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val userProfile = userPreferences.userProfile
    val isOnlineConnected: StateFlow<Boolean> = messageRepository.isOnlineConnected

    init {
        viewModelScope.launch {
            userPreferences.userProfile.collectLatest { profile ->
                if (profile.phoneNumber.isNotBlank()) {
                    messageRepository.startOnlineSync(profile.phoneNumber)
                }
            }
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val conversations: StateFlow<List<ConversationEntity>> = combine(
        messageRepository.getAllConversations(),
        _searchQuery
    ) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            list.filter { conv ->
                (conv.contactName?.contains(query, ignoreCase = true) == true) ||
                        conv.phoneNumber.contains(query) ||
                        conv.lastMessage.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun syncSmsAndContacts(context: Context) {
        viewModelScope.launch {
            contactRepository.syncDeviceContacts(context)
            messageRepository.syncDeviceSms(userProfile.value.phoneNumber)
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            messageRepository.deleteConversation(conversationId)
        }
    }
}
