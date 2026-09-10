package com.example.ui.chat

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.QuickResponseEntity
import com.example.data.local.entity.ScheduledMessageEntity
import com.example.data.model.SimCardInfo
import com.example.data.preference.AppSettings
import com.example.data.preference.UserPreferences
import com.example.data.repository.ContactRepository
import com.example.data.repository.MessageRepository
import com.example.ui.util.SimManagerHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DelayedSendState(
    val id: String,
    val content: String,
    val totalSeconds: Int,
    val remainingSeconds: Float,
    val isCountingDown: Boolean = true
)

class ChatViewModel(
    private val conversationId: String,
    private val messageRepository: MessageRepository,
    private val contactRepository: ContactRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val appSettings: StateFlow<AppSettings> = userPreferences.appSettings

    private val _conversation = MutableStateFlow<ConversationEntity?>(null)
    val conversation: StateFlow<ConversationEntity?> = _conversation.asStateFlow()

    private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messages: StateFlow<List<MessageEntity>> = _messages.asStateFlow()

    val scheduledMessages: StateFlow<List<ScheduledMessageEntity>> =
        messageRepository.getScheduledMessagesForConversation(conversationId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quickResponses: StateFlow<List<QuickResponseEntity>> =
        messageRepository.getAllQuickResponses()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _availableSims = MutableStateFlow<List<SimCardInfo>>(emptyList())
    val availableSims: StateFlow<List<SimCardInfo>> = _availableSims.asStateFlow()

    private val _selectedSimIndex = MutableStateFlow(0)
    val selectedSimIndex: StateFlow<Int> = _selectedSimIndex.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _reactions = MutableStateFlow<Map<String, String>>(emptyMap())
    val reactions: StateFlow<Map<String, String>> = _reactions.asStateFlow()

    private val _delayedSendState = MutableStateFlow<DelayedSendState?>(null)
    val delayedSendState: StateFlow<DelayedSendState?> = _delayedSendState.asStateFlow()

    private var delayedSendJob: Job? = null

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

        // Check if any due scheduled messages exist
        viewModelScope.launch {
            messageRepository.checkAndDispatchDueScheduledMessages()
        }
    }

    fun loadSims(context: Context) {
        val sims = SimManagerHelper.getActiveSimCards(context)
        _availableSims.value = sims
        if (_selectedSimIndex.value >= sims.size) {
            _selectedSimIndex.value = 0
        }
    }

    fun selectSim(index: Int) {
        if (index in _availableSims.value.indices) {
            _selectedSimIndex.value = index
        }
    }

    fun cycleNextSim() {
        val sims = _availableSims.value
        if (sims.size > 1) {
            _selectedSimIndex.value = (_selectedSimIndex.value + 1) % sims.size
        }
    }

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun appendQuickResponse(text: String) {
        val current = _inputText.value
        _inputText.value = if (current.isBlank()) text else "$current $text"
    }

    fun addNewQuickResponse(text: String) {
        if (text.isNotBlank()) {
            viewModelScope.launch {
                messageRepository.addQuickResponse(text)
            }
        }
    }

    fun deleteQuickResponse(id: Long) {
        viewModelScope.launch {
            messageRepository.deleteQuickResponse(id)
        }
    }

    fun addReaction(messageId: String, emoji: String) {
        val current = _reactions.value.toMutableMap()
        current[messageId] = emoji
        _reactions.value = current
    }

    fun initiateSendMessage(context: Context, mediaUrl: String? = null) {
        val text = _inputText.value.trim()
        if (text.isEmpty() && mediaUrl == null) return

        val delaySec = appSettings.value.sendDelaySeconds

        // If delayed send is enabled and it's a text message, start delayed send timer
        if (delaySec > 0 && mediaUrl == null) {
            val contentToSend = text
            _inputText.value = "" // clear input

            delayedSendJob?.cancel()
            delayedSendJob = viewModelScope.launch {
                val sendId = System.currentTimeMillis().toString()
                val totalSteps = delaySec * 10
                for (i in totalSteps downTo 0) {
                    val remaining = i / 10f
                    _delayedSendState.value = DelayedSendState(
                        id = sendId,
                        content = contentToSend,
                        totalSeconds = delaySec,
                        remainingSeconds = remaining,
                        isCountingDown = true
                    )
                    delay(100)
                }

                // If timer reaches 0 and wasn't cancelled, actually dispatch
                _delayedSendState.value = null
                actuallyDispatchMessage(context, contentToSend, null)
            }
        } else {
            val contentToSend = if (text.isNotBlank()) text else "Attachment"
            _inputText.value = ""
            actuallyDispatchMessage(context, contentToSend, mediaUrl)
        }
    }

    fun cancelDelayedSend() {
        val state = _delayedSendState.value ?: return
        delayedSendJob?.cancel()
        _delayedSendState.value = null
        _inputText.value = state.content // Restore text for editing
    }

    fun sendDelayedNow(context: Context) {
        val state = _delayedSendState.value ?: return
        delayedSendJob?.cancel()
        _delayedSendState.value = null
        actuallyDispatchMessage(context, state.content, null)
    }

    private fun actuallyDispatchMessage(context: Context, content: String, mediaUrl: String?) {
        val conv = _conversation.value
        val recipientPhone = conv?.phoneNumber ?: conversationId
        val recipientName = conv?.contactName

        // Vibrate on send if enabled
        if (appSettings.value.vibrateOnSend) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(50)
                    }
                }
            } catch (_: Exception) {
                // Ignore vibration failure
            }
        }

        // Append signature if configured
        val signature = appSettings.value.signatureText.trim()
        val finalContent = if (signature.isNotEmpty() && mediaUrl == null) {
            "$content\n$signature"
        } else {
            content
        }

        val currentSim = _availableSims.value.getOrNull(_selectedSimIndex.value)
        val subId = currentSim?.subscriptionId

        viewModelScope.launch {
            messageRepository.sendMessage(
                recipientPhone = recipientPhone,
                recipientName = recipientName,
                content = finalContent,
                mediaUrl = mediaUrl,
                subscriptionId = subId
            )
        }
    }

    fun scheduleMessage(scheduledTimestamp: Long) {
        val text = _inputText.value.trim()
        if (text.isEmpty()) return

        val conv = _conversation.value
        val recipientPhone = conv?.phoneNumber ?: conversationId
        val recipientName = conv?.contactName

        viewModelScope.launch {
            messageRepository.scheduleMessage(
                recipientPhone = recipientPhone,
                recipientName = recipientName,
                content = text,
                scheduledTimestamp = scheduledTimestamp
            )
            _inputText.value = ""
        }
    }

    fun cancelScheduledMessage(id: Long) {
        viewModelScope.launch {
            messageRepository.cancelScheduledMessage(id)
        }
    }

    fun sendScheduledMessageNow(scheduled: ScheduledMessageEntity) {
        viewModelScope.launch {
            messageRepository.sendScheduledMessageNow(
                id = scheduled.id,
                recipientPhone = scheduled.recipientPhoneNumber,
                recipientName = scheduled.recipientName,
                content = scheduled.content
            )
        }
    }

    fun togglePin() {
        val conv = _conversation.value ?: return
        viewModelScope.launch {
            messageRepository.setPinned(conv.conversationId, !conv.isPinned)
        }
    }

    fun blockContact() {
        val conv = _conversation.value ?: return
        viewModelScope.launch {
            messageRepository.blockContact(conv.phoneNumber, conv.contactName)
        }
    }

    fun setCustomColor(colorHex: String?) {
        val conv = _conversation.value ?: return
        viewModelScope.launch {
            messageRepository.setConversationCustomColor(conv.conversationId, colorHex)
        }
    }

    fun retryMessage(messageId: String) {
        viewModelScope.launch {
            messageRepository.retryMessage(messageId)
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            messageRepository.deleteMessage(messageId)
        }
    }
}
