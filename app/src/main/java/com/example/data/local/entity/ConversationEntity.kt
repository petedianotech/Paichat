package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversations",
    indices = [
        Index(value = ["isBlocked", "isPinned", "lastMessageTimestamp"]),
        Index(value = ["phoneNumber"]),
        Index(value = ["isInBin"])
    ]
)
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
    val customColorHex: String? = null,
    val customWallpaper: String? = null,
    val isInBin: Boolean = false,
    val deletedTimestamp: Long = 0L
)
