package com.ugandai.ugandai.chat.data.dao

import androidx.room.*
import com.ugandai.ugandai.chat.data.entity.MessageEntity

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE username = :username ORDER BY created_at ASC")
    suspend fun getMessages(username: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE messages SET message_status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: String)
}
