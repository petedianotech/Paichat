package com.example.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.TrashMessageEntity
import com.example.data.model.SyncProgress
import com.example.data.preference.AppSettings
import com.example.data.preference.UserPreferences
import com.example.data.repository.ContactRepository
import com.example.data.repository.MessageRepository
import com.example.ui.util.DraftManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HomeFilter {
    ALL,
    UNREAD,
    PINNED,
    DRAFTS,
    TRASH
}

class HomeViewModel(
    private val messageRepository: MessageRepository,
    private val contactRepository: ContactRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val appSettings: StateFlow<AppSettings> = userPreferences.appSettings
    val syncProgress: StateFlow<SyncProgress> = messageRepository.syncProgress
    val contactsMap = contactRepository.contactsMap
    val drafts = DraftManager.drafts
    val trash: StateFlow<List<TrashMessageEntity>> = messageRepository.getTrash()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentFilter = MutableStateFlow(HomeFilter.ALL)
    val currentFilter: StateFlow<HomeFilter> = _currentFilter.asStateFlow()

    val conversations: StateFlow<List<ConversationEntity>> = combine(
        messageRepository.getAllConversations(),
        _searchQuery,
        _currentFilter,
        drafts
    ) { list, query, filter, draftMap ->
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
            HomeFilter.DRAFTS -> queryFiltered.filter { !draftMap[it.conversationId].isNullOrBlank() }
            HomeFilter.TRASH -> emptyList()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val matchedMessages: StateFlow<List<MessageEntity>> = _searchQuery.flatMapLatest { query ->
        if (query.trim().isNotEmpty()) {
            messageRepository.searchMessages(query.trim())
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun getPhotoUriForPhone(phoneNumber: String): String? {
        return contactRepository.getPhotoUriForPhoneNumber(phoneNumber)
    }

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
            launch {
                contactRepository.syncDeviceContacts(context)
            }
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

    fun deleteConversationWithUndo(conversationId: String, onUndoAvailable: (() -> Unit) -> Unit) {
        viewModelScope.launch {
            val conv = messageRepository.getConversationDirect(conversationId)
            val msgs = messageRepository.getMessagesDirect(conversationId)
            messageRepository.deleteConversation(conversationId)

            if (conv != null) {
                onUndoAvailable {
                    viewModelScope.launch {
                        messageRepository.restoreConversationAndMessages(conv, msgs)
                    }
                }
            }
        }
    }

    fun restoreFromTrash(message: TrashMessageEntity) {
        viewModelScope.launch {
            messageRepository.restoreFromTrash(message)
        }
    }

    fun permanentlyDeleteFromTrash(message: TrashMessageEntity) {
        viewModelScope.launch {
            messageRepository.permanentlyDeleteFromTrash(message)
        }
    }
}
