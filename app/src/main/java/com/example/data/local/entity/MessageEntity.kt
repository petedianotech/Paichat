package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["senderPhoneNumber"]),
        Index(value = ["recipientPhoneNumber"]),
        Index(value = ["timestamp"])
    ]
)
data class MessageEntity(
    @PrimaryKey val messageId: String,
    val conversationId: String,
    val senderPhoneNumber: String,
    val recipientPhoneNumber: String,
    val content: String,
    val timestamp: Long,
    val messageType: MessageType,
    val status: MessageStatus,
    val mediaUrl: String? = null
)

