package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.DeletedSmsEntity
import com.example.data.local.entity.TrashMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrashDao {
    @Query("SELECT * FROM trash_messages ORDER BY deletedAt DESC")
    fun getTrash(): Flow<List<TrashMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrashMessages(messages: List<TrashMessageEntity>)

    @Query("DELETE FROM trash_messages WHERE messageId = :messageId")
    suspend fun deleteTrashMessage(messageId: String)

    @Query("DELETE FROM trash_messages WHERE messageId IN (:messageIds)")
    suspend fun deleteTrashMessages(messageIds: List<String>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDeletedSms(messages: List<DeletedSmsEntity>)

    @Query("SELECT messageId FROM deleted_sms")
    suspend fun getDeletedSmsIds(): List<String>
}
