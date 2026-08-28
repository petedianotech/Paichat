package com.example.ui.navigation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.database.PulseChatDatabase
import com.example.data.preference.UserPreferences
import com.example.data.repository.ContactRepository
import com.example.data.repository.MessageRepository
import com.example.ui.chat.ChatViewModel
import com.example.ui.home.HomeViewModel
import com.example.ui.newchat.NewChatViewModel
import com.example.ui.onboarding.OnboardingViewModel
import com.example.ui.profile.ProfileViewModel

class PulseChatViewModelFactory(
    private val context: Context,
    private val conversationId: String? = null
) : ViewModelProvider.Factory {

    private val userPreferences by lazy { UserPreferences(context) }
    private val contactRepository by lazy { ContactRepository() }
    private val database by lazy { PulseChatDatabase.getDatabase(context) }
    private val messageRepository by lazy {
        MessageRepository(
            context = context,
            conversationDao = database.conversationDao(),
            messageDao = database.messageDao(),
            contactRepository = contactRepository
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(OnboardingViewModel::class.java) -> {
                OnboardingViewModel(userPreferences, messageRepository) as T
            }
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(messageRepository, contactRepository, userPreferences) as T
            }
            modelClass.isAssignableFrom(NewChatViewModel::class.java) -> {
                NewChatViewModel(contactRepository) as T
            }
            modelClass.isAssignableFrom(ChatViewModel::class.java) -> {
                requireNotNull(conversationId) { "conversationId is required for ChatViewModel" }
                ChatViewModel(conversationId, messageRepository, contactRepository, userPreferences) as T
            }
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
                ProfileViewModel(userPreferences, messageRepository, contactRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
