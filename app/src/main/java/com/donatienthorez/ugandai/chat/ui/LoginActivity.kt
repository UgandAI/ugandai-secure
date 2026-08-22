package com.donatienthorez.ugandai.chat.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ugandai.ugandai.auth.ui.AuthScreen

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AuthScreen(
                onLoginSuccess = { token ->
                    val intent = Intent(this, com.ugandai.ugandai.profile.ui.ProfileActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            )
        }
    }
}
