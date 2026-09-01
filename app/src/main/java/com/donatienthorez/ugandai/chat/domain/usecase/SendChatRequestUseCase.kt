package com.ugandai.ugandai.chat.domain.usecase

import com.ugandai.ugandai.chat.data.ConversationRepository
import com.ugandai.ugandai.chat.data.Message
import com.ugandai.ugandai.chat.data.MessageStatus
import com.donatienthorez.ugandai.chat.data.api.OpenAIRepository
import com.donatienthorez.ugandai.chat.data.api.ChatStreamEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException

sealed interface ChatTurnResult {
    data class Success(val conversationId: Int, val assistantText: String) : ChatTurnResult
    data class Failure(val message: String) : ChatTurnResult
}

class SendChatRequestUseCase(
    private val openAIRepository: OpenAIRepository,
    private val conversationRepository: ConversationRepository
) {

    suspend operator fun invoke(
        prompt: String
    ): ChatTurnResult {
        val conversationId = conversationRepository.selectedConversationOrCreate()
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

        val assistantText = StringBuilder()
        return try {
            openAIRepository.sendChatRequestStream(message.text, conversationId).collect { event ->
                when (event) {
                    is ChatStreamEvent.Content -> {
                        assistantText.append(event.text)
                        conversationRepository.updateMessageText(assistantMessage.id, event.text)
                    }
                    is ChatStreamEvent.Citations -> conversationRepository.updateMessageCitations(assistantMessage.id, event.items)
                }
            }
            conversationRepository.setMessageStatusToSent(message.id)
            conversationRepository.setMessageStatusToSent(assistantMessage.id)
            conversationRepository.refreshConversations()
            ChatTurnResult.Success(conversationId, assistantText.toString())
        } catch (exception: CancellationException) {
            conversationRepository.setMessageStatusToError(message.id)
            conversationRepository.setMessageStatusToError(assistantMessage.id)
            throw exception
        } catch (exception: Exception) {
            conversationRepository.setMessageStatusToError(message.id)
            conversationRepository.setMessageStatusToError(assistantMessage.id)
            ChatTurnResult.Failure(exception.message ?: "Unable to get a response")
        }
    }
}
