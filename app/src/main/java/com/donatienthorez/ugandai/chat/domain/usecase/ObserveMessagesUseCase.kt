package com.donatienthorez.ugandai.chat.domain.usecase

import com.ugandai.ugandai.chat.data.ConversationRepository

class ObserveMessagesUseCase(
    private val conversationRepository: ConversationRepository
) {

    operator fun invoke() = conversationRepository.conversationFlow

}