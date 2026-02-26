package com.ugandai.ugandai.chat.data

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.donatienthorez.ugandai.chat.ui.DatabaseHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import com.donatienthorez.ugandai.chat.data.api.ProposedActivity


class ConversationRepository(private val context: Context) {

    private val dbHelper = DatabaseHelper(context)
    private var messagesList = mutableListOf<Message>()
    private var currentUsername: String? = null

    init {
        currentUsername = getCurrentUsername()
        ensureMessagesTableExists()
        loadMessagesFromDatabase()
        
        if (messagesList.isEmpty()) {
            val welcomeMessage = Message(
                text = "Welcome farmer, how can I help?",
                isFromUser = false,
                messageStatus = MessageStatus.Sent
            )
            messagesList.add(welcomeMessage)
            saveMessageToDatabase(welcomeMessage)
        }
    }

    private val _conversationFlow = MutableStateFlow(
        value = Conversation(list = messagesList)
    )
    val conversationFlow = _conversationFlow.asStateFlow()

    fun addMessage(message: Message) : Conversation {
        messagesList.add(message)
        saveMessageToDatabase(message)
        return updateConversationFlow(messagesList)
    }

    fun resendMessage(message: Message) : Conversation {
        messagesList.remove(message)
        messagesList.add(message)
        return updateConversationFlow(messagesList)
    }

    fun setMessageStatusToSent(messageId: String) {
        val index = messagesList.indexOfFirst { it.id == messageId }
        if (index != -1) {
            messagesList[index] = messagesList[index].copy(messageStatus = MessageStatus.Sent)
            updateMessageStatusInDatabase(messageId, "Sent")
        }

        updateConversationFlow(messagesList)
    }

    fun setMessageStatusToError(messageId: String) {
        val index = messagesList.indexOfFirst { it.id == messageId }
        if (index != -1) {
            messagesList[index] = messagesList[index].copy(messageStatus = MessageStatus.Error)
            updateMessageStatusInDatabase(messageId, "Error")
        }

        updateConversationFlow(messagesList)
    }

    private fun updateConversationFlow(messagesList: List<Message>) : Conversation {
        val conversation = Conversation(
            list = messagesList
        )
        _conversationFlow.value = conversation

        return conversation
    }

    private fun getCurrentUsername(): String? {
        return try {
            val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val prefs = EncryptedSharedPreferences.create(
                "secure_prefs",
                masterKey,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            prefs.getString("username", null)
        } catch (e: Exception) {
            null
        }
    }

    private fun ensureMessagesTableExists() {
        try {
            val db = dbHelper.writableDatabase
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS messages(" +
                        "id TEXT PRIMARY KEY, " +
                        "username TEXT NOT NULL, " +
                        "text TEXT NOT NULL, " +
                        "is_from_user INTEGER NOT NULL, " +
                        "message_status TEXT NOT NULL, " +
                        "proposed_activity_type TEXT, " +
                        "proposed_date TEXT, " +
                        "proposed_crop TEXT, " +
                        "proposed_note TEXT, " +
                        "proposed_confidence REAL, " +
                        "created_at INTEGER NOT NULL)"
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadMessagesFromDatabase() {
        val username = currentUsername ?: return
        
        try {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery(
                "SELECT * FROM messages WHERE username = ? ORDER BY created_at ASC",
                arrayOf(username)
            )

            val messages = mutableListOf<Message>()
            while (cursor.moveToNext()) {
                val id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
                val text = cursor.getString(cursor.getColumnIndexOrThrow("text"))
                val isFromUser = cursor.getInt(cursor.getColumnIndexOrThrow("is_from_user")) == 1
                val statusString = cursor.getString(cursor.getColumnIndexOrThrow("message_status"))
                val status = when (statusString) {
                    "Sent" -> MessageStatus.Sent
                    "Error" -> MessageStatus.Error
                    else -> MessageStatus.Sending
                }

                // Load proposed activity if exists
                val activityTypeIndex = cursor.getColumnIndexOrThrow("proposed_activity_type")
                val proposedActivity = if (!cursor.isNull(activityTypeIndex)) {
                    ProposedActivity(
                        activityType = cursor.getString(activityTypeIndex),
                        date = cursor.getString(cursor.getColumnIndexOrThrow("proposed_date")),
                        crop = cursor.getString(cursor.getColumnIndexOrThrow("proposed_crop")),
                        note = cursor.getString(cursor.getColumnIndexOrThrow("proposed_note")),
                        confidence = cursor.getDouble(cursor.getColumnIndexOrThrow("proposed_confidence"))
                    )
                } else null

                messages.add(
                    Message(
                        id = id,
                        text = text,
                        isFromUser = isFromUser,
                        messageStatus = status,
                        proposedActivity = proposedActivity
                    )
                )
            }
            cursor.close()
            messagesList.addAll(messages)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveMessageToDatabase(message: Message) {
        val username = currentUsername ?: return
        
        try {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("id", message.id)
                put("username", username)
                put("text", message.text)
                put("is_from_user", if (message.isFromUser) 1 else 0)
                put("message_status", when (message.messageStatus) {
                    is MessageStatus.Sent -> "Sent"
                    is MessageStatus.Error -> "Error"
                    is MessageStatus.Sending -> "Sending"
                })
                put("proposed_activity_type", message.proposedActivity?.activityType)
                put("proposed_date", message.proposedActivity?.date)
                put("proposed_crop", message.proposedActivity?.crop)
                put("proposed_note", message.proposedActivity?.note)
                put("proposed_confidence", message.proposedActivity?.confidence)
                put("created_at", System.currentTimeMillis())
            }
            db.insert("messages", null, values)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateMessageStatusInDatabase(messageId: String, status: String) {
        try {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("message_status", status)
            }
            db.update("messages", values, "id = ?", arrayOf(messageId))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class Conversation(
    val list: List<Message>
)

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isFromUser: Boolean,
    val messageStatus: MessageStatus = MessageStatus.Sending,
    val proposedActivity: ProposedActivity? = null
)

sealed class MessageStatus {
    object Sending: MessageStatus()
    object Error: MessageStatus()
    object Sent: MessageStatus()
}