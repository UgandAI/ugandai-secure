package com.donatienthorez.ugandai.chat.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.koin.androidx.viewmodel.ext.android.stateViewModel
import com.ugandai.ugandai.chat.ui.ChatScreen
import com.ugandai.ugandai.chat.ui.ChatScreenUiHandlers
import com.ugandai.ugandai.chat.ui.ChatViewModel
import com.ugandai.ugandai.logbook.ui.LogBookActivity
import com.ugandai.ugandai.ui.ChatGptBotAppTheme

class ChatActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by stateViewModel(
        state = { intent?.extras ?: Bundle() }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val presetPrompt = intent.getStringExtra("preset_prompt")

        setContent {
            ChatGptBotAppTheme {
                ChatScreen(
                    uiHandlers = ChatScreenUiHandlers(
                        onSendMessage = { prompt ->
                            viewModel.sendMessage(prompt)
                        },

                        onResendMessage = viewModel::resendMessage,

                        // ✅ FIXED EXTRA KEYS HERE
                        onAddToLogBook = { message ->

                            val proposed = message.proposedActivity
                            if (proposed != null) {

                                val intent = Intent(this, LogBookActivity::class.java).apply {
                                    putExtra(LogBookActivity.EXTRA_ACTIVITY_TYPE, proposed.activityType)
                                    putExtra(LogBookActivity.EXTRA_DATE, proposed.date)
                                    putExtra(LogBookActivity.EXTRA_CROP, proposed.crop)
                                    putExtra(LogBookActivity.EXTRA_FIELD, "") // not extracted yet
                                    putExtra(LogBookActivity.EXTRA_NOTE, proposed.note ?: "")
                                }

                                startActivity(intent)
                            }
                        },

                        onNavigateToLogBook = {
                            startActivity(Intent(this, LogBookActivity::class.java))
                        }
                    ),
                    conversation = viewModel.conversation,
                    isSendingMessage = viewModel.isSendingMessage
                )
            }
        }

        presetPrompt?.let {
            viewModel.sendMessage(it)
        }
    }
}