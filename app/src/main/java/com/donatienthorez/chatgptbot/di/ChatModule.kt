package com.ugandai.chatgptbot.di

import com.ugandai.chatgptbot.chat.data.ConversationRepository
import com.ugandai.chatgptbot.chat.data.api.OpenAIRepository
import com.ugandai.chatgptbot.chat.domain.usecase.ObserveMessagesUseCase
import com.ugandai.chatgptbot.chat.domain.usecase.ResendMessageUseCase
import com.ugandai.chatgptbot.chat.domain.usecase.SendChatRequestUseCase
import com.ugandai.chatgptbot.chat.ui.ChatViewModel
import org.koin.dsl.module
import org.koin.androidx.viewmodel.dsl.viewModel

val chatModule = module {
    viewModel {
        ChatViewModel(get(), get(), get())
    }
    single { OpenAIRepository(openAI = get()) }
    single { ConversationRepository() }

    single { SendChatRequestUseCase(openAIRepository = get(), conversationRepository = get()) }
    single { ResendMessageUseCase(openAIRepository = get(), conversationRepository = get()) }
    single { ObserveMessagesUseCase(conversationRepository = get()) }
}