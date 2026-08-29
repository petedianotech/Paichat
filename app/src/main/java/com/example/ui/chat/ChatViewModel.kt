package com.example.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.MessageType
import com.example.data.preference.UserPreferences
import com.example.data.repository.ContactRepository
import com.example.data.repository.MessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class MessageReaction(
    val messageId: String,
    val emoji: String
)

class ChatViewModel(
    private val conversationId: String,
    private val messageRepository: MessageRepository,
    private val contactRepository: ContactRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val userProfile = userPreferences.userProfile
    val isOnlineConnected: StateFlow<Boolean> = messageRepository.isOnlineConnected

    private val _conversation = MutableStateFlow<ConversationEntity?>(null)
    val conversation: StateFlow<ConversationEntity?> = _conversation.asStateFlow()

    private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messages: StateFlow<List<MessageEntity>> = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isForcedSms = MutableStateFlow(false)
    val isForcedSms: StateFlow<Boolean> = _isForcedSms.asStateFlow()

    private val _reactions = MutableStateFlow<Map<String, String>>(emptyMap())
    val reactions: StateFlow<Map<String, String>> = _reactions.asStateFlow()

    private val _replyingToMessage = MutableStateFlow<MessageEntity?>(null)
    val replyingToMessage: StateFlow<MessageEntity?> = _replyingToMessage.asStateFlow()

    init {
        viewModelScope.launch {
            messageRepository.clearUnreadCount(conversationId)
            messageRepository.getConversationById(conversationId).collectLatest {
                _conversation.value = it
            }
        }

        viewModelScope.launch {
            messageRepository.getMessagesForConversation(conversationId).collectLatest {
                _messages.value = it
            }
        }
    }

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun toggleForcedSms() {
        _isForcedSms.value = !_isForcedSms.value
    }

    fun setReplyingTo(message: MessageEntity?) {
        _replyingToMessage.value = message
    }

    fun addReaction(messageId: String, emoji: String) {
        val current = _reactions.value.toMutableMap()
        current[messageId] = emoji
        _reactions.value = current
    }

    fun sendMessage(mediaUrl: String? = null) {
        val text = _inputText.value.trim()
        if (text.isEmpty() && mediaUrl == null) return

        val myPhone = userProfile.value.phoneNumber
        val conv = _conversation.value
        val recipientPhone = conv?.phoneNumber ?: conversationId
        val recipientName = conv?.contactName

        val forcedType = if (_isForcedSms.value) MessageType.SMS else null

        viewModelScope.launch {
            messageRepository.sendMessage(
                senderPhone = myPhone,
                recipientPhone = recipientPhone,
                recipientName = recipientName,
                content = if (text.isNotBlank()) text else "Attachment",
                forcedMessageType = forcedType,
                mediaUrl = mediaUrl
            )
            _inputText.value = ""
            _replyingToMessage.value = null
        }
    }

    fun fallbackToSendAsSms(messageId: String) {
        viewModelScope.launch {
            messageRepository.fallbackToSendAsSms(messageId)
        }
    }
}
