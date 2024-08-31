package com.ugandai.chatgptbot.chat.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ugandai.chatgptbot.chat.data.Conversation
import com.ugandai.chatgptbot.chat.data.Message
import com.ugandai.chatgptbot.chat.data.MessageStatus
import com.donatienthorez.chatgptbot.chat.domain.usecase.ObserveMessagesUseCase
import com.ugandai.chatgptbot.chat.domain.usecase.ResendMessageUseCase
import com.ugandai.chatgptbot.chat.domain.usecase.SendChatRequestUseCase
import kotlinx.coroutines.launch

class ChatViewModel(
    private val sendChatRequestUseCase: SendChatRequestUseCase,
    private val resendChatRequestUseCase: ResendMessageUseCase,
    private val observeMessagesUseCase: ObserveMessagesUseCase,
) : ViewModel() {

    private val _conversation = MutableLiveData<Conversation>()
    val conversation: LiveData<Conversation> = _conversation

    private val _isSendingMessage = MutableLiveData<Boolean>()
    val isSendingMessage: LiveData<Boolean> = _isSendingMessage

    init {
        observeMessageList()
    }

    private fun observeMessageList() {
        viewModelScope.launch {
            observeMessagesUseCase.invoke().collect { conversation ->
                _conversation.postValue(conversation)

                _isSendingMessage.postValue(
                    conversation.list.any { it.messageStatus == MessageStatus.Sending }
                )
            }
        }
    }

    fun sendMessage(prompt: String, vectorStoreId: String) {
        viewModelScope.launch {
            // Create a user message from the prompt
            val userMessage = Message(
                text = prompt,
                isFromUser = true, // Indicates this message is from the user
                messageStatus = MessageStatus.Sending // Initial status might be Sending
            )

            // Create a system message with vector store instructions
            val instructions = "Please use the vector store with ID $vectorStoreId to answer any questions related to the content of the files."
            val systemMessage = Message(
                text = instructions,
                isFromUser = false, // Indicates this message is from the system or assistant
                messageStatus = MessageStatus.Sending // Initial status might be Sending
            )

            // Create a Conversation object with the list of messages
            val conversation = Conversation(
                listOf(userMessage, systemMessage)
            )

            // Call the sendChatRequestUseCase with the initialized conversation and vectorStoreId
            sendChatRequestUseCase.invoke(prompt)
        }
    }

    fun resendMessage(message: Message) {
        viewModelScope.launch {
            resendChatRequestUseCase.invoke(message)
        }
    }
}
