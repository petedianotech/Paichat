package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.QuickResponseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuickResponseDao {
    @Query("SELECT * FROM quick_responses ORDER BY orderIndex ASC, id ASC")
    fun getAllQuickResponses(): Flow<List<QuickResponseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuickResponse(response: QuickResponseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuickResponses(responses: List<QuickResponseEntity>)

    @Update
    suspend fun updateQuickResponse(response: QuickResponseEntity)

    @Query("DELETE FROM quick_responses WHERE id = :id")
    suspend fun deleteQuickResponse(id: Long)

    @Query("SELECT COUNT(*) FROM quick_responses")
    suspend fun getCount(): Int
}
