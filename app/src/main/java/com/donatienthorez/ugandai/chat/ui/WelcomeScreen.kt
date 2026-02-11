package com.donatienthorez.ugandai.chat.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ugandai.ugandai.R

@Composable
fun WelcomeScreen(
    onLoginClick: () -> Unit,
    onEmailSignupClick: () -> Unit
) {

    // 🔥 Entrance animation trigger
    var startAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    // 🌊 Floating logo animation
    val infiniteTransition = rememberInfiniteTransition()
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {

        // 🌌 Background
        Image(
            painter = painterResource(id = R.drawable.welcome_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 🌑 Cinematic gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.65f)
                        )
                    )
                )
        )

        AnimatedVisibility(
            visible = startAnimation,
            enter = fadeIn(animationSpec = tween(1000)) +
                    slideInVertically(
                        initialOffsetY = { 40 },
                        animationSpec = tween(900)
                    )
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 48.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // 🔝 TOP SECTION
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    Spacer(modifier = Modifier.height(32.dp))

                    // ✨ Glowing circle behind logo
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(140.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF2E4B3C).copy(alpha = 0.6f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    ) {

                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = null,
                            modifier = Modifier
                                .size(110.dp)
                                .offset(y = floatOffset.dp)
                                .clip(CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = "UgandAI",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFF2E6C9)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Powering the Future of Farming.",
                        fontSize = 18.sp,
                        color = Color(0xFFE0C07A)
                    )
                }

                // 🔽 BOTTOM SECTION
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    GlassButton(
                        text = "Login",
                        backgroundColor = Color.Black.copy(alpha = 0.65f),
                        textColor = Color.White,
                        onClick = onLoginClick
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    GlassButton(
                        text = "Sign up with Email",
                        backgroundColor = Color(0xFF1E2F23).copy(alpha = 0.9f),
                        textColor = Color(0xFFE0C07A),
                        onClick = onEmailSignupClick
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "By continuing you agree to the Terms of Service and Privacy Policy.",
                        fontSize = 12.sp,
                        color = Color(0xFFB8A67A)
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassButton(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(120)
    )

    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(40),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = textColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .scale(scale)
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}