package com.example

import android.app.Application
import android.content.Context
import com.example.data.local.database.PulseChatDatabase
import com.example.data.preference.UserPreferences
import com.example.data.repository.ContactRepository
import com.example.data.repository.MessageRepository
import com.example.ui.util.DraftManager

/**
 * Singleton Application Container for PulseChat.
 * Centralizes repository and database lifecycles to prevent multiple instance allocations,
 * reduce background RAM footprint to <15MB, and prevent background crashes.
 */
class PulseChatApp : Application() {

    lateinit var userPreferences: UserPreferences
        private set

    lateinit var database: PulseChatDatabase
        private set

    lateinit var contactRepository: ContactRepository
        private set

    lateinit var messageRepository: MessageRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        userPreferences = UserPreferences(applicationContext)
        database = PulseChatDatabase.getDatabase(applicationContext)
        contactRepository = ContactRepository()
        DraftManager.init(applicationContext)
        messageRepository = MessageRepository(
            context = applicationContext,
            conversationDao = database.conversationDao(),
            messageDao = database.messageDao(),
            scheduledMessageDao = database.scheduledMessageDao(),
            blockedContactDao = database.blockedContactDao(),
            quickResponseDao = database.quickResponseDao(),
            contactRepository = contactRepository,
            userPreferences = userPreferences,
            trashDao = database.trashDao()
        )
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_MODERATE) {
            contactRepository.clearCaches()
        }
    }

    companion object {
        @Volatile
        private var instance: PulseChatApp? = null

        fun getInstance(): PulseChatApp {
            return checkNotNull(instance) { "PulseChatApp is not initialized yet" }
        }

        fun getMessageRepository(context: Context): MessageRepository {
            return (context.applicationContext as? PulseChatApp)?.messageRepository
                ?: getInstance().messageRepository
        }

        fun getContactRepository(context: Context): ContactRepository {
            return (context.applicationContext as? PulseChatApp)?.contactRepository
                ?: getInstance().contactRepository
        }

        fun getUserPreferences(context: Context): UserPreferences {
            return (context.applicationContext as? PulseChatApp)?.userPreferences
                ?: getInstance().userPreferences
        }

        fun getDatabase(context: Context): PulseChatDatabase {
            return (context.applicationContext as? PulseChatApp)?.database
                ?: getInstance().database
        }
    }
}
