package com.ugandai.ugandai.profile.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.donatienthorez.ugandai.chat.ui.ChatActivity

class ProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProfileScreen(
                onProfileSaved = {
                    val intent = Intent(this, ChatActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            )
        }
    }
}
