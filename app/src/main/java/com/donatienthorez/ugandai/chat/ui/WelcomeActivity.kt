package com.donatienthorez.ugandai.chat.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent


class WelcomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WelcomeScreen(
                {
                    startActivity(Intent(this, LoginActivity::class.java))
                    Unit
                },
                {
                    startActivity(Intent(this, SignupActivity::class.java))
                    Unit
                }
            )
        }
    }
}