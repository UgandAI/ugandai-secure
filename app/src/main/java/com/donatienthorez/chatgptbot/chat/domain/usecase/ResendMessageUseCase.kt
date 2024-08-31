package com.ugandai.chatgptbot.chat.domain.usecase

import com.ugandai.chatgptbot.chat.data.ConversationRepository
import com.ugandai.chatgptbot.chat.data.Message
import com.ugandai.chatgptbot.chat.data.MessageStatus
import com.ugandai.chatgptbot.chat.data.api.OpenAIRepository

class ResendMessageUseCase(
    private val openAIRepository: OpenAIRepository,
    private val conversationRepository: ConversationRepository
) {

    suspend operator fun invoke(
        message: Message
    ) {
        val conversation = conversationRepository.resendMessage(message)

        try {
            val reply = openAIRepository.sendChatRequest(conversation, message.text)
            conversationRepository.setMessageStatusToSent(message.id)
            conversationRepository.addMessage(reply)
        } catch (exception: Exception) {
            conversationRepository.setMessageStatusToError(message.id)
        }
    }
}