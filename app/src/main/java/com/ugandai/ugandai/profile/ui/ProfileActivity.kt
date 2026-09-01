package com.ugandai.ugandai.profile.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.donatienthorez.ugandai.chat.ui.ChatActivity
import com.ugandai.ugandai.auth.data.AuthTokenStore
import com.ugandai.ugandai.data.api.UgandAIApiClient
import kotlinx.coroutines.launch

class ProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!AuthTokenStore(this).token().isNullOrBlank()) {
            lifecycleScope.launch {
                try {
                    if (UgandAIApiClient.api.getFarmProfiles().isNotEmpty()) {
                        startActivity(Intent(this@ProfileActivity, ChatActivity::class.java))
                        finish()
                    }
                } catch (_: Exception) {
                    // Keep profile setup visible; it already reports save/network errors.
                }
            }
        }
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
