package com.example.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.local.converter.Converters
import com.example.data.local.dao.BlockedContactDao
import com.example.data.local.dao.ConversationDao
import com.example.data.local.dao.MessageDao
import com.example.data.local.dao.QuickResponseDao
import com.example.data.local.dao.ScheduledMessageDao
import com.example.data.local.entity.BlockedContactEntity
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.QuickResponseEntity
import com.example.data.local.entity.ScheduledMessageEntity

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        ScheduledMessageEntity::class,
        BlockedContactEntity::class,
        QuickResponseEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PulseChatDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun scheduledMessageDao(): ScheduledMessageDao
    abstract fun blockedContactDao(): BlockedContactDao
    abstract fun quickResponseDao(): QuickResponseDao

    companion object {
        @Volatile
        private var INSTANCE: PulseChatDatabase? = null

        fun getDatabase(context: Context): PulseChatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PulseChatDatabase::class.java,
                    "pulse_chat_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
