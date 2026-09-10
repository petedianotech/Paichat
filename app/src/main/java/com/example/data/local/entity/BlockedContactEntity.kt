package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_contacts")
data class BlockedContactEntity(
    @PrimaryKey val phoneNumber: String,
    val contactName: String?,
    val blockedTimestamp: Long = System.currentTimeMillis(),
    val reason: String? = null
)
