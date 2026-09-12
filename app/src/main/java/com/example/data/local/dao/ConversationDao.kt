package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations WHERE isBlocked = 0 ORDER BY isPinned DESC, lastMessageTimestamp DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE isBlocked = 1 ORDER BY lastMessageTimestamp DESC")
    fun getBlockedConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE conversationId = :id")
    fun getConversationById(id: String): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE conversationId = :id")
    suspend fun getConversationByIdDirect(id: String): ConversationEntity?

    @Query("SELECT * FROM conversations")
    suspend fun getAllConversationsDirect(): List<ConversationEntity>

    @Query("SELECT * FROM conversations WHERE phoneNumber = :phoneNumber LIMIT 1")
    suspend fun getConversationByPhoneNumber(phoneNumber: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(conversations: List<ConversationEntity>)

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Query("UPDATE conversations SET isPinned = :isPinned WHERE conversationId = :id")
    suspend fun setPinned(id: String, isPinned: Boolean)

    @Query("UPDATE conversations SET isBlocked = :isBlocked WHERE conversationId = :id")
    suspend fun setBlocked(id: String, isBlocked: Boolean)

    @Query("UPDATE conversations SET customColorHex = :colorHex WHERE conversationId = :id")
    suspend fun setCustomColor(id: String, colorHex: String?)

    @Query("UPDATE conversations SET customWallpaper = :wallpaper WHERE conversationId = :id")
    suspend fun setCustomWallpaper(id: String, wallpaper: String?)

    @Query("DELETE FROM conversations WHERE conversationId = :id")
    suspend fun deleteConversationById(id: String)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE conversationId = :id")
    suspend fun clearUnreadCount(id: String)

    @Query("UPDATE conversations SET unreadCount = 1 WHERE conversationId = :id")
    suspend fun markAsUnread(id: String)

    @Query("UPDATE conversations SET isInternetUser = :isInternetUser WHERE conversationId = :id")
    suspend fun updateInternetUserStatus(id: String, isInternetUser: Boolean)
}
