package com.donatienthorez.ugandai.chat.ui.presets

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.donatienthorez.ugandai.chat.ui.ChatActivity
import com.ugandai.ugandai.ui.ChatGptBotAppTheme

class PresetPromptsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatGptBotAppTheme {
                PresetPromptsScreen(
                    onPromptSelected = { prompt ->
                        val intent = Intent(this@PresetPromptsActivity, ChatActivity::class.java)
                        intent.putExtra("preset_prompt", prompt)
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}
