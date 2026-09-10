package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scheduled_messages")
data class ScheduledMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: String,
    val recipientPhoneNumber: String,
    val recipientName: String?,
    val content: String,
    val scheduledTimestamp: Long,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val isSent: Boolean = false
)
