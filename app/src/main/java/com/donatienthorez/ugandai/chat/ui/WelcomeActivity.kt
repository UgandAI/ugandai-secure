package com.donatienthorez.ugandai.chat.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ugandai.ugandai.auth.data.AuthTokenStore
import com.ugandai.ugandai.profile.ui.ProfileActivity


class WelcomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!AuthTokenStore(this).token().isNullOrBlank()) {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
            return
        }

        setContent {
            WelcomeScreen(
                {
                    startActivity(Intent(this, LoginActivity::class.java))
                    Unit
                },
                {
                    startActivity(Intent(this, LoginActivity::class.java))
                    Unit
                }
            )
        }
    }
}
