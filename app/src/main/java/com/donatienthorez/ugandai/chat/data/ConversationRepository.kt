package com.ugandai.ugandai.chat.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.ugandai.ugandai.chat.data.dao.MessageDao
import com.ugandai.ugandai.chat.data.entity.toDomain
import com.ugandai.ugandai.chat.data.entity.toEntity
import com.donatienthorez.ugandai.chat.data.api.ProposedActivity
import com.donatienthorez.ugandai.chat.data.api.Citation
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
    private val _conversationSummaries = MutableStateFlow<List<ConversationSummary>>(emptyList())
    val conversationSummaries = _conversationSummaries.asStateFlow()
    private val _selectedConversationId = MutableStateFlow<Int?>(null)
    val selectedConversationId = _selectedConversationId.asStateFlow()

    init {
        currentUsername = getCurrentUsername()
        repositoryScope.launch { refreshConversations(selectMostRecent = true) }
    }

    private val _conversationFlow = MutableStateFlow(
        value = Conversation(list = messagesList)
    )
    val conversationFlow = _conversationFlow.asStateFlow()

    suspend fun refreshConversations(selectMostRecent: Boolean = false) {
        val remote = com.ugandai.ugandai.data.api.UgandAIApiClient.api.getConversations()
        _conversationSummaries.value = remote.map {
            ConversationSummary(it.id, it.title, it.created_at, it.updated_at)
        }
        if (selectMostRecent && _selectedConversationId.value == null) {
            if (remote.isEmpty()) newConversation() else selectConversation(remote.first().id)
        }
    }

    suspend fun newConversation() {
        val created = com.ugandai.ugandai.data.api.UgandAIApiClient.api.createConversation()
        _conversationSummaries.value = listOf(
            ConversationSummary(created.id, created.title, created.created_at, created.updated_at)
        ) + _conversationSummaries.value.filterNot { it.id == created.id }
        _selectedConversationId.value = created.id
        synchronized(messagesList) { messagesList.clear() }
        updateConversationFlow(messagesList)
    }

    suspend fun selectConversation(id: Int) {
        val remote = com.ugandai.ugandai.data.api.UgandAIApiClient.api.getConversationMessages(id)
        val loaded = remote.map {
            Message(
                id = "server-${it.id}", text = it.content,
                isFromUser = it.role == "user", messageStatus = MessageStatus.Sent
            )
        }
        _selectedConversationId.value = id
        synchronized(messagesList) {
            messagesList.clear()
            messagesList.addAll(loaded)
        }
        updateConversationFlow(messagesList)
    }

    suspend fun selectedConversationOrCreate(): Int {
        if (_selectedConversationId.value == null) newConversation()
        return requireNotNull(_selectedConversationId.value)
    }

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

    fun updateMessageText(messageId: String, newChunk: String) {
        synchronized(messagesList) {
            val index = messagesList.indexOfFirst { it.id == messageId }
            if (index != -1) {
                val currentText = messagesList[index].text
                val updatedText = currentText + newChunk
                messagesList[index] = messagesList[index].copy(text = updatedText)
                repositoryScope.launch {
                    saveMessageToDatabase(messagesList[index])
                }
            }
        }
        updateConversationFlow(messagesList)
    }

    /** Sets a message's full text (as opposed to [updateMessageText], which appends a streamed chunk). */
    fun replaceMessageText(messageId: String, newText: String) {
        synchronized(messagesList) {
            val index = messagesList.indexOfFirst { it.id == messageId }
            if (index != -1) {
                messagesList[index] = messagesList[index].copy(text = newText)
                repositoryScope.launch {
                    saveMessageToDatabase(messagesList[index])
                }
            }
        }
        updateConversationFlow(messagesList)
    }

    fun updateMessageCitations(messageId: String, citations: List<Citation>) {
        synchronized(messagesList) {
            val index = messagesList.indexOfFirst { it.id == messageId }
            if (index != -1) messagesList[index] = messagesList[index].copy(citations = citations)
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

data class ConversationSummary(
    val id: Int,
    val title: String,
    val createdAt: String,
    val updatedAt: String
)

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isFromUser: Boolean,
    val messageStatus: MessageStatus = MessageStatus.Sending,
    val proposedActivity: ProposedActivity? = null,
    val citations: List<Citation> = emptyList()
)

sealed class MessageStatus {
    object Sending: MessageStatus()
    object Error: MessageStatus()
    object Sent: MessageStatus()
}
