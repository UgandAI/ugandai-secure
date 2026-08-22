package com.ugandai.ugandai.chat.domain.usecase

import com.ugandai.ugandai.chat.data.ConversationRepository
import com.ugandai.ugandai.chat.data.Message
import com.ugandai.ugandai.chat.data.MessageStatus
import com.donatienthorez.ugandai.chat.data.api.OpenAIRepository

class ResendMessageUseCase(
    private val openAIRepository: OpenAIRepository,
    private val conversationRepository: ConversationRepository
) {

    suspend operator fun invoke(
        message: Message
    ) {
        conversationRepository.resendMessage(message)
        
        // Add an empty assistant message to stream into
        val assistantMessage = Message(
            text = "",
            isFromUser = false,
            messageStatus = MessageStatus.Sending
        )
        conversationRepository.addMessage(assistantMessage)

        try {
            openAIRepository.sendChatRequestStream(message.text).collect { chunk ->
                conversationRepository.updateMessageText(assistantMessage.id, chunk)
            }
            conversationRepository.setMessageStatusToSent(message.id)
            conversationRepository.setMessageStatusToSent(assistantMessage.id)
        } catch (exception: Exception) {
            conversationRepository.setMessageStatusToError(message.id)
            conversationRepository.setMessageStatusToError(assistantMessage.id)
        }
    }
}