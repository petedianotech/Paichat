package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trash_messages")
data class TrashMessageEntity(
    @PrimaryKey val messageId: String,
    val conversationId: String,
    val senderPhoneNumber: String,
    val recipientPhoneNumber: String,
    val content: String,
    val timestamp: Long,
    val messageType: MessageType,
    val status: MessageStatus,
    val mediaUrl: String?,
    val deletedAt: Long
)
