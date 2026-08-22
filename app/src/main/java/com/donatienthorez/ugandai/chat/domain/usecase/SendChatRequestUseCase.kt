package com.ugandai.ugandai.chat.domain.usecase

import com.ugandai.ugandai.chat.data.ConversationRepository
import com.ugandai.ugandai.chat.data.Message
import com.ugandai.ugandai.chat.data.MessageStatus
import com.donatienthorez.ugandai.chat.data.api.OpenAIRepository
import kotlinx.coroutines.delay

class SendChatRequestUseCase(
    private val openAIRepository: OpenAIRepository,
    private val conversationRepository: ConversationRepository
) {

    suspend operator fun invoke(
        prompt: String
    ) {
        val message = Message(
            text = prompt,
            isFromUser = true,
            messageStatus = MessageStatus.Sending
        )
        conversationRepository.addMessage(message)
        
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