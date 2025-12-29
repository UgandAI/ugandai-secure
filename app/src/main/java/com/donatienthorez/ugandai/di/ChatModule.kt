package com.ugandai.ugandai.di

import com.ugandai.ugandai.chat.data.ConversationRepository
import com.ugandai.ugandai.chat.data.api.OpenAIRepository
import com.donatienthorez.ugandai.chat.domain.usecase.ObserveMessagesUseCase
import com.ugandai.ugandai.chat.domain.usecase.ResendMessageUseCase
import com.ugandai.ugandai.chat.domain.usecase.SendChatRequestUseCase
import com.ugandai.ugandai.chat.ui.ChatViewModel
import com.ugandai.ugandai.logbook.data.LogBookRepository
import com.ugandai.ugandai.logbook.ui.LogBookViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.koin.androidx.viewmodel.dsl.viewModel

val chatModule = module {
    viewModel {
        ChatViewModel(get(), get(), get())
    }

    // Provide OpenAIRepository with context
    single { OpenAIRepository(context = androidContext()) }

    single { ConversationRepository() }

    single { SendChatRequestUseCase(openAIRepository = get(), conversationRepository = get()) }
    single { ResendMessageUseCase(openAIRepository = get(), conversationRepository = get()) }
    single { ObserveMessagesUseCase(conversationRepository = get()) }

    // LogBook
    single { LogBookRepository(androidContext()) }
    viewModel { LogBookViewModel(get(), androidContext()) }
}
