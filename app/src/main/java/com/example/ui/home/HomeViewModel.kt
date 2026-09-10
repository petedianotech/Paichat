package com.example.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.ConversationEntity
import com.example.data.preference.AppSettings
import com.example.data.preference.UserPreferences
import com.example.data.repository.ContactRepository
import com.example.data.repository.MessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HomeFilter {
    ALL,
    UNREAD,
    PINNED
}

class HomeViewModel(
    private val messageRepository: MessageRepository,
    private val contactRepository: ContactRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val appSettings: StateFlow<AppSettings> = userPreferences.appSettings

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentFilter = MutableStateFlow(HomeFilter.ALL)
    val currentFilter: StateFlow<HomeFilter> = _currentFilter.asStateFlow()

    val conversations: StateFlow<List<ConversationEntity>> = combine(
        messageRepository.getAllConversations(),
        _searchQuery,
        _currentFilter
    ) { list, query, filter ->
        val queryFiltered = if (query.isBlank()) {
            list
        } else {
            list.filter { conv ->
                (conv.contactName?.contains(query, ignoreCase = true) == true) ||
                        conv.phoneNumber.contains(query) ||
                        conv.lastMessage.contains(query, ignoreCase = true)
            }
        }

        when (filter) {
            HomeFilter.ALL -> queryFiltered
            HomeFilter.UNREAD -> queryFiltered.filter { it.unreadCount > 0 }
            HomeFilter.PINNED -> queryFiltered.filter { it.isPinned }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // Automatically check for due scheduled messages on launch
        viewModelScope.launch {
            messageRepository.checkAndDispatchDueScheduledMessages()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: HomeFilter) {
        _currentFilter.value = filter
    }

    fun syncSmsAndContacts(context: Context) {
        viewModelScope.launch {
            contactRepository.syncDeviceContacts(context)
            messageRepository.syncDeviceSms()
        }
    }

    fun quickCompose(phone: String, text: String, onSent: () -> Unit) {
        if (phone.isNotBlank() && text.isNotBlank()) {
            viewModelScope.launch {
                val contact = contactRepository.getContactByPhoneNumber(phone)
                messageRepository.sendMessage(
                    recipientPhone = phone.trim(),
                    recipientName = contact?.name,
                    content = text.trim()
                )
                onSent()
            }
        }
    }

    fun togglePin(conversationId: String, currentPinned: Boolean) {
        viewModelScope.launch {
            messageRepository.setPinned(conversationId, !currentPinned)
        }
    }

    fun markAsReadOrUnread(conversationId: String, currentUnread: Int) {
        viewModelScope.launch {
            if (currentUnread > 0) {
                messageRepository.clearUnreadCount(conversationId)
            } else {
                messageRepository.markAsUnread(conversationId)
            }
        }
    }

    fun blockContact(phoneNumber: String, contactName: String?) {
        viewModelScope.launch {
            messageRepository.blockContact(phoneNumber, contactName)
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            messageRepository.deleteConversation(conversationId)
        }
    }
}
