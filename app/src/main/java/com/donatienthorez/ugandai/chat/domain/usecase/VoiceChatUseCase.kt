package com.ugandai.ugandai.chat.domain.usecase

import com.donatienthorez.ugandai.chat.data.api.OpenAIRepository
import com.donatienthorez.ugandai.chat.data.audio.VoicePlayer
import com.ugandai.ugandai.chat.data.ConversationRepository
import com.ugandai.ugandai.chat.data.Message
import com.ugandai.ugandai.chat.data.MessageStatus
import java.io.File

/** Uploads a recorded voice message to POST /voice/chat, then renders the transcript,
 * reply text, and citations into the conversation and plays the synthesized reply audio. */
class VoiceChatUseCase(
    private val openAIRepository: OpenAIRepository,
    private val conversationRepository: ConversationRepository,
    private val voicePlayer: VoicePlayer
) {

    suspend operator fun invoke(audioFile: File) {
        val userMessage = Message(text = "🎤 Voice message", isFromUser = true, messageStatus = MessageStatus.Sending)
        conversationRepository.addMessage(userMessage)

        val assistantMessage = Message(text = "", isFromUser = false, messageStatus = MessageStatus.Sending)
        conversationRepository.addMessage(assistantMessage)

        try {
            val result = openAIRepository.sendVoiceChat(audioFile.readBytes())
            conversationRepository.replaceMessageText(userMessage.id, result.transcript)
            conversationRepository.replaceMessageText(assistantMessage.id, result.content)
            if (result.citations.isNotEmpty()) {
                conversationRepository.updateMessageCitations(assistantMessage.id, result.citations)
            }
            conversationRepository.setMessageStatusToSent(userMessage.id)
            conversationRepository.setMessageStatusToSent(assistantMessage.id)
            voicePlayer.play(result.audioBytes, result.audioFormat)
        } catch (exception: Exception) {
            conversationRepository.setMessageStatusToError(userMessage.id)
            conversationRepository.setMessageStatusToError(assistantMessage.id)
        } finally {
            audioFile.delete()
        }
    }
}
