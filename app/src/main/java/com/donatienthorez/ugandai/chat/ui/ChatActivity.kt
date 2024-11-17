package com.donatienthorez.ugandai.chat.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import org.koin.androidx.viewmodel.ext.android.stateViewModel
import androidx.activity.compose.setContent
import com.ugandai.ugandai.chat.ui.ChatScreen
import com.ugandai.ugandai.chat.ui.ChatScreenUiHandlers
import com.ugandai.ugandai.chat.ui.ChatViewModel
import com.ugandai.ugandai.ui.ChatGptBotAppTheme

class ChatActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by stateViewModel(
        state = { intent?.extras ?: Bundle() }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatGptBotAppTheme {
                ChatScreen(
                    uiHandlers = ChatScreenUiHandlers(
                        onSendMessage = { prompt -> viewModel.sendMessage(prompt, "your_vector_store_id_here") },
                        onResendMessage = viewModel::resendMessage
                    ),
                    conversation = viewModel.conversation,
                    isSendingMessage = viewModel.isSendingMessage
                )
            }
        }
    }
}
