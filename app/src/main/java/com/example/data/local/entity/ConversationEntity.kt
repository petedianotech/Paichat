package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val conversationId: String,
    val phoneNumber: String,
    val contactName: String?,
    val lastMessage: String,
    val lastMessageTimestamp: Long,
    val unreadCount: Int = 0,
    val isInternetUser: Boolean = false,
    val isPinned: Boolean = false,
    val isBlocked: Boolean = false,
    val customColorHex: String? = null
)
