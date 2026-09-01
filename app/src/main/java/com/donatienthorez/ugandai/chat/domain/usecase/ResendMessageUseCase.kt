package com.ugandai.ugandai.chat.domain.usecase

import com.ugandai.ugandai.chat.data.ConversationRepository
import com.ugandai.ugandai.chat.data.Message
import com.ugandai.ugandai.chat.data.MessageStatus
import com.donatienthorez.ugandai.chat.data.api.OpenAIRepository
import com.donatienthorez.ugandai.chat.data.api.ChatStreamEvent

class ResendMessageUseCase(
    private val openAIRepository: OpenAIRepository,
    private val conversationRepository: ConversationRepository
) {

    suspend operator fun invoke(
        message: Message
    ) {
        val conversationId = conversationRepository.selectedConversationOrCreate()
        conversationRepository.resendMessage(message)
        
        // Add an empty assistant message to stream into
        val assistantMessage = Message(
            text = "",
            isFromUser = false,
            messageStatus = MessageStatus.Sending
        )
        conversationRepository.addMessage(assistantMessage)

        try {
            openAIRepository.sendChatRequestStream(message.text, conversationId).collect { event ->
                when (event) {
                    is ChatStreamEvent.Content -> conversationRepository.updateMessageText(assistantMessage.id, event.text)
                    is ChatStreamEvent.Citations -> conversationRepository.updateMessageCitations(assistantMessage.id, event.items)
                }
            }
            conversationRepository.setMessageStatusToSent(message.id)
            conversationRepository.setMessageStatusToSent(assistantMessage.id)
            conversationRepository.refreshConversations()
        } catch (exception: Exception) {
            conversationRepository.setMessageStatusToError(message.id)
            conversationRepository.setMessageStatusToError(assistantMessage.id)
        }
    }
}
