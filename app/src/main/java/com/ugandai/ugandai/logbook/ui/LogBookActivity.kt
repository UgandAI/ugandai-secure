package com.ugandai.ugandai.logbook.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.ugandai.ugandai.ui.ChatGptBotAppTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class LogBookActivity : ComponentActivity() {

    private val viewModel: LogBookViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatGptBotAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LogBookScreen(viewModel = viewModel)
                }
            }
        }
    }
}
