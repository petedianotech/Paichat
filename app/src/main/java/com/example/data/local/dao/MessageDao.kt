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
        WHERE conversationId = :conversationId 
           OR conversationId = :normalizedId
           OR recipientPhoneNumber = :conversationId
           OR recipientPhoneNumber = :normalizedId
           OR senderPhoneNumber = :conversationId
           OR senderPhoneNumber = :normalizedId
        ORDER BY timestamp ASC
    """)
    fun getMessagesForConversationFlexible(conversationId: String, normalizedId: String): Flow<List<MessageEntity>>

    @Query("""
        SELECT * FROM messages 
        WHERE conversationId = :conversationId 
           OR conversationId = :normalizedId
           OR recipientPhoneNumber = :conversationId
           OR recipientPhoneNumber = :normalizedId
           OR senderPhoneNumber = :conversationId
           OR senderPhoneNumber = :normalizedId
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
        WHERE conversationId = :conversationId 
           OR conversationId = :normalizedId
           OR recipientPhoneNumber = :conversationId
           OR recipientPhoneNumber = :normalizedId
           OR senderPhoneNumber = :conversationId
           OR senderPhoneNumber = :normalizedId
        ORDER BY timestamp ASC
    """)
    suspend fun getMessagesForConversationFlexibleDirect(conversationId: String, normalizedId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMessageForConversation(conversationId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE messageId = :messageId")
    suspend fun getMessageById(messageId: String): MessageEntity?

    @Query("""
        SELECT * FROM messages 
        WHERE senderPhoneNumber = :sender 
          AND content = :content 
          AND timestamp >= :sinceTimestamp 
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

    @Query("SELECT * FROM messages WHERE content LIKE '%' || :query || '%' ORDER BY timestamp DESC LIMIT 50")
    fun searchMessages(query: String): Flow<List<MessageEntity>>

    @Query("DELETE FROM messages WHERE messageId = :messageId")
    suspend fun deleteMessageById(messageId: String)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: String)
}
