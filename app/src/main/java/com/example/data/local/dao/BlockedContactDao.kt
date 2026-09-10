package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.BlockedContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedContactDao {
    @Query("SELECT * FROM blocked_contacts ORDER BY blockedTimestamp DESC")
    fun getAllBlocked(): Flow<List<BlockedContactEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_contacts WHERE phoneNumber = :phoneNumber)")
    suspend fun isBlocked(phoneNumber: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun block(contact: BlockedContactEntity)

    @Query("DELETE FROM blocked_contacts WHERE phoneNumber = :phoneNumber")
    suspend fun unblock(phoneNumber: String)
}
