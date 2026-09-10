package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quick_responses")
data class QuickResponseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val isDefault: Boolean = false,
    val orderIndex: Int = 0
)
