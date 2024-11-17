package com.ugandai.ugandai.chat.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ugandai.ugandai.chat.data.Conversation
import com.ugandai.ugandai.chat.data.Message
import com.ugandai.ugandai.chat.data.MessageStatus
import com.donatienthorez.ugandai.chat.domain.usecase.ObserveMessagesUseCase
import com.ugandai.ugandai.chat.domain.usecase.ResendMessageUseCase
import com.ugandai.ugandai.chat.domain.usecase.SendChatRequestUseCase
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
                messageStatus = MessageStatus.Sending
            )


            val instructions = "Please use the vector store with ID $vectorStoreId to answer any questions related to the content of the files."
            val systemMessage = Message(
                text = instructions,
                isFromUser = false,
                messageStatus = MessageStatus.Sending
            )


            val conversation = Conversation(
                listOf(userMessage, systemMessage)
            )


            sendChatRequestUseCase.invoke(prompt)
        }
    }

    fun resendMessage(message: Message) {
        viewModelScope.launch {
            resendChatRequestUseCase.invoke(message)
        }
    }
}
