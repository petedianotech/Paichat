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
    @Query("SELECT * FROM conversations WHERE isBlocked = 0 AND isInBin = 0 ORDER BY isPinned DESC, lastMessageTimestamp DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE isBlocked = 1 AND isInBin = 0 ORDER BY lastMessageTimestamp DESC")
    fun getBlockedConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE conversationId = :id")
    fun getConversationById(id: String): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE conversationId = :id")
    suspend fun getConversationByIdDirect(id: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE isInBin = 0")
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

    @Query("UPDATE conversations SET contactName = :name WHERE conversationId = :id")
    suspend fun updateContactName(id: String, name: String)

    @Query("DELETE FROM conversations WHERE conversationId = :id")
    suspend fun deleteConversationById(id: String)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE conversationId = :id")
    suspend fun clearUnreadCount(id: String)

    @Query("UPDATE conversations SET unreadCount = 1 WHERE conversationId = :id")
    suspend fun markAsUnread(id: String)

    @Query("UPDATE conversations SET isInternetUser = :isInternetUser WHERE conversationId = :id")
    suspend fun updateInternetUserStatus(id: String, isInternetUser: Boolean)

    // ==========================================
    // RECYCLE BIN OPERATIONS
    // ==========================================

    @Query("SELECT * FROM conversations WHERE isInBin = 1 ORDER BY deletedTimestamp DESC, lastMessageTimestamp DESC")
    fun getConversationsInBin(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE isInBin = 1 ORDER BY deletedTimestamp DESC, lastMessageTimestamp DESC")
    suspend fun getConversationsInBinDirect(): List<ConversationEntity>

    @Query("SELECT COUNT(*) FROM conversations WHERE isInBin = 1")
    fun getBinConversationCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM conversations WHERE isInBin = 1")
    suspend fun getBinConversationCountDirect(): Int

    @Query("UPDATE conversations SET isInBin = 1, deletedTimestamp = :timestamp WHERE conversationId = :id")
    suspend fun moveConversationToBin(id: String, timestamp: Long)

    @Query("UPDATE conversations SET isInBin = 0, deletedTimestamp = 0 WHERE conversationId = :id")
    suspend fun restoreConversationFromBin(id: String)

    @Query("DELETE FROM conversations WHERE conversationId = :id")
    suspend fun permanentlyDeleteConversation(id: String)

    @Query("DELETE FROM conversations WHERE isInBin = 1")
    suspend fun emptyConversationBin()
}
