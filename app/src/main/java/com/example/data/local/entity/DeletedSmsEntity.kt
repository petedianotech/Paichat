package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deleted_sms")
data class DeletedSmsEntity(
    @PrimaryKey val messageId: String,
    val deletedAt: Long
)
