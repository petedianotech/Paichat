package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.MessageStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("""
        SELECT * FROM messages 
        WHERE ((conversationId = :conversationId 
           OR conversationId = :normalizedId
           OR recipientPhoneNumber = :conversationId
           OR recipientPhoneNumber = :normalizedId
           OR senderPhoneNumber = :conversationId
           OR senderPhoneNumber = :normalizedId))
          AND isInBin = 0
        ORDER BY timestamp ASC
    """)
    fun getMessagesForConversationFlexible(conversationId: String, normalizedId: String): Flow<List<MessageEntity>>

    @Query("""
        SELECT * FROM messages 
        WHERE ((conversationId = :conversationId 
           OR conversationId = :normalizedId
           OR recipientPhoneNumber = :conversationId
           OR recipientPhoneNumber = :normalizedId
           OR senderPhoneNumber = :conversationId
           OR senderPhoneNumber = :normalizedId))
          AND isInBin = 0
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    fun getMessagesForConversationFlexiblePaged(
        conversationId: String, 
        normalizedId: String, 
        limit: Int
    ): Flow<List<MessageEntity>>

    @Query("""
        SELECT * FROM messages 
        WHERE ((conversationId = :conversationId 
           OR conversationId = :normalizedId
           OR recipientPhoneNumber = :conversationId
           OR recipientPhoneNumber = :normalizedId
           OR senderPhoneNumber = :conversationId
           OR senderPhoneNumber = :normalizedId))
          AND isInBin = 0
        ORDER BY timestamp ASC
    """)
    suspend fun getMessagesForConversationFlexibleDirect(conversationId: String, normalizedId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND isInBin = 0 ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("""
        SELECT * FROM messages 
        WHERE (conversationId = :conversationId 
           OR conversationId = :normalizedId
           OR recipientPhoneNumber = :conversationId
           OR recipientPhoneNumber = :normalizedId
           OR senderPhoneNumber = :conversationId
           OR senderPhoneNumber = :normalizedId)
          AND isInBin = 0 
        ORDER BY timestamp DESC 
        LIMIT 1
    """)
    suspend fun getLastMessageForConversationFlexible(conversationId: String, normalizedId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND isInBin = 0 ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMessageForConversation(conversationId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE messageId = :messageId")
    suspend fun getMessageById(messageId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE messageId IN (:messageIds)")
    suspend fun getMessagesByIds(messageIds: List<String>): List<MessageEntity>

    @Query("SELECT COUNT(*) FROM messages WHERE isInBin = 0")
    suspend fun getMessageCount(): Int

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM messages 
            WHERE (conversationId = :conversationId OR conversationId = :normalizedId)
              AND content = :content 
              AND timestamp >= :minTimestamp 
              AND timestamp <= :maxTimestamp
        )
    """)
    suspend fun hasSimilarMessage(
        conversationId: String,
        normalizedId: String,
        content: String,
        minTimestamp: Long,
        maxTimestamp: Long
    ): Boolean

    @Query("""
        SELECT * FROM messages 
        WHERE senderPhoneNumber = :sender 
          AND content = :content 
          AND timestamp >= :sinceTimestamp 
          AND isInBin = 0
        LIMIT 1
    """)
    suspend fun findRecentIncomingMessage(sender: String, content: String, sinceTimestamp: Long): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessagesIgnore(messages: List<MessageEntity>)

    @Query("UPDATE messages SET status = :status WHERE messageId = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE content LIKE '%' || :query || '%' AND isInBin = 0 ORDER BY timestamp DESC LIMIT 50")
    fun searchMessages(query: String): Flow<List<MessageEntity>>

    @Query("DELETE FROM messages WHERE messageId = :messageId")
    suspend fun deleteMessageById(messageId: String)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: String)

    // ==========================================
    // RECYCLE BIN OPERATIONS
    // ==========================================

    @Query("SELECT * FROM messages WHERE isInBin = 1 ORDER BY deletedTimestamp DESC, timestamp DESC")
    fun getMessagesInBin(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE isInBin = 1 ORDER BY deletedTimestamp DESC, timestamp DESC")
    suspend fun getMessagesInBinDirect(): List<MessageEntity>

    @Query("SELECT COUNT(*) FROM messages WHERE isInBin = 1")
    fun getBinMessageCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM messages WHERE isInBin = 1")
    suspend fun getBinMessageCountDirect(): Int

    @Query("UPDATE messages SET isInBin = 1, deletedTimestamp = :timestamp WHERE messageId = :messageId")
    suspend fun moveMessageToBin(messageId: String, timestamp: Long)

    @Query("UPDATE messages SET isInBin = 1, deletedTimestamp = :timestamp WHERE messageId IN (:messageIds)")
    suspend fun moveMessagesToBin(messageIds: List<String>, timestamp: Long)

    @Query("""
        UPDATE messages SET isInBin = 1, deletedTimestamp = :timestamp 
        WHERE conversationId = :conversationId 
           OR conversationId = :normalizedId
           OR recipientPhoneNumber = :conversationId
           OR recipientPhoneNumber = :normalizedId
           OR senderPhoneNumber = :conversationId
           OR senderPhoneNumber = :normalizedId
    """)
    suspend fun moveConversationMessagesToBin(conversationId: String, normalizedId: String, timestamp: Long)

    @Query("UPDATE messages SET isInBin = 0, deletedTimestamp = 0 WHERE messageId = :messageId")
    suspend fun restoreMessageFromBin(messageId: String)

    @Query("UPDATE messages SET isInBin = 0, deletedTimestamp = 0 WHERE messageId IN (:messageIds)")
    suspend fun restoreMessagesFromBin(messageIds: List<String>)

    @Query("""
        UPDATE messages SET isInBin = 0, deletedTimestamp = 0 
        WHERE conversationId = :conversationId 
           OR conversationId = :normalizedId
           OR recipientPhoneNumber = :conversationId
           OR recipientPhoneNumber = :normalizedId
           OR senderPhoneNumber = :conversationId
           OR senderPhoneNumber = :normalizedId
    """)
    suspend fun restoreConversationMessagesFromBin(conversationId: String, normalizedId: String)

    @Query("DELETE FROM messages WHERE messageId IN (:messageIds)")
    suspend fun permanentlyDeleteMessages(messageIds: List<String>)

    @Query("DELETE FROM messages WHERE isInBin = 1")
    suspend fun emptyBin()
}
