package com.ugandai.ugandai.chat.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.ugandai.ugandai.chat.data.dao.MessageDao
import com.ugandai.ugandai.chat.data.entity.toDomain
import com.ugandai.ugandai.chat.data.entity.toEntity
import com.donatienthorez.ugandai.chat.data.api.ProposedActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ConversationRepository(
    private val context: Context,
    private val messageDao: MessageDao
) {

    private var messagesList = mutableListOf<Message>()
    private var currentUsername: String? = null
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    init {
        currentUsername = getCurrentUsername()
        repositoryScope.launch {
            loadMessagesFromDatabase()
            synchronized(messagesList) {
                if (messagesList.isEmpty()) {
                    val welcomeMessage = Message(
                        text = "Welcome farmer, how can I help?",
                        isFromUser = false,
                        messageStatus = MessageStatus.Sent
                    )
                    messagesList.add(welcomeMessage)
                    repositoryScope.launch {
                        saveMessageToDatabase(welcomeMessage)
                    }
                }
            }
            updateConversationFlow(messagesList)
        }
    }

    private val _conversationFlow = MutableStateFlow(
        value = Conversation(list = messagesList)
    )
    val conversationFlow = _conversationFlow.asStateFlow()

    fun addMessage(message: Message) : Conversation {
        synchronized(messagesList) {
            messagesList.add(message)
        }
        repositoryScope.launch {
            saveMessageToDatabase(message)
        }
        return updateConversationFlow(messagesList)
    }

    fun resendMessage(message: Message) : Conversation {
        synchronized(messagesList) {
            messagesList.remove(message)
            messagesList.add(message)
        }
        return updateConversationFlow(messagesList)
    }

    fun setMessageStatusToSent(messageId: String) {
        synchronized(messagesList) {
            val index = messagesList.indexOfFirst { it.id == messageId }
            if (index != -1) {
                messagesList[index] = messagesList[index].copy(messageStatus = MessageStatus.Sent)
                repositoryScope.launch {
                    updateMessageStatusInDatabase(messageId, "Sent")
                }
            }
        }
        updateConversationFlow(messagesList)
    }

    fun setMessageStatusToError(messageId: String) {
        synchronized(messagesList) {
            val index = messagesList.indexOfFirst { it.id == messageId }
            if (index != -1) {
                messagesList[index] = messagesList[index].copy(messageStatus = MessageStatus.Error)
                repositoryScope.launch {
                    updateMessageStatusInDatabase(messageId, "Error")
                }
            }
        }
        updateConversationFlow(messagesList)
    }

    private fun updateConversationFlow(messagesList: List<Message>) : Conversation {
        val conversation = Conversation(
            list = messagesList.toList()
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

    private suspend fun loadMessagesFromDatabase() {
        val username = currentUsername ?: return
        try {
            val entities = messageDao.getMessages(username)
            val messages = entities.map { it.toDomain() }
            synchronized(messagesList) {
                messagesList.clear()
                messagesList.addAll(messages)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun saveMessageToDatabase(message: Message) {
        val username = currentUsername ?: return
        try {
            messageDao.insertMessage(message.toEntity(username, System.currentTimeMillis()))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun updateMessageStatusInDatabase(messageId: String, status: String) {
        try {
            messageDao.updateMessageStatus(messageId, status)
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