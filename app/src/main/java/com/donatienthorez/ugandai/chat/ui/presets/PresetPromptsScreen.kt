package com.donatienthorez.ugandai.chat.ui.presets

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

data class PresetPrompt(
    val text: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetPromptsScreen(
    onPromptSelected: (String) -> Unit
) {

    val prompts = listOf(
        PresetPrompt(
            "What crops should I plant this season in Mbale region?",
            Icons.Default.Agriculture
        ),
        PresetPrompt(
            "How do I prepare my soil for maize planting?",
            Icons.Default.Eco
        ),
        PresetPrompt(
            "What are the best practices for coffee farming in Uganda?",
            Icons.Default.WbSunny
        ),
        PresetPrompt(
            "Help me plan my planting schedule based on current weather",
            Icons.Default.Schedule
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {

        // 🌅 Premium Forest → Morning Glow Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F241C),
                            Color(0xFF1E3A2F),
                            Color(0xFF2E5A45),
                            Color(0xFFF2E6C9)
                        )
                    )
                )
        )

        // ✨ Faster Floating Vertical Particles
        val infiniteTransition = rememberInfiniteTransition(label = "")

        val animatedAlpha by infiniteTransition.animateFloat(
            initialValue = 0.08f,
            targetValue = 0.22f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500),
                repeatMode = RepeatMode.Reverse
            ),
            label = ""
        )

        val animatedOffset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 160f,
            animationSpec = infiniteRepeatable(
                animation = tween(3500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = ""
        )

        val particles = remember {
            List(28) {
                Triple(
                    Random.nextFloat(),
                    Random.nextFloat(),
                    Random.nextFloat() * 4f + 2f
                )
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { (x, y, radius) ->
                drawCircle(
                    color = Color.White.copy(alpha = animatedAlpha),
                    radius = radius,
                    center = Offset(
                        x * size.width,
                        (y * size.height + animatedOffset) % size.height
                    )
                )
            }
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "How can I help you today?",
                            fontWeight = FontWeight.Medium,
                            fontSize = 24.sp,
                            letterSpacing = 0.8.sp,
                            color = Color(0xFFF8F4E6)
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 22.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(prompts) { prompt ->

                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()

                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.96f else 1f,
                        animationSpec = spring(
                            dampingRatio = 0.6f,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = ""
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(185.dp)
                            .scale(scale)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                onPromptSelected(prompt.text)
                            },
                        shape = RoundedCornerShape(30.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.07f)
                        )
                    ) {

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.06f),
                                            Color.Transparent
                                        ),
                                        radius = 700f
                                    )
                                )
                                .padding(28.dp),
                            contentAlignment = Alignment.Center
                        ) {

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {

                                Icon(
                                    imageVector = prompt.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(46.dp),
                                    tint = Color(0xFFE0C07A)
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                    text = prompt.text,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    letterSpacing = 0.4.sp,
                                    textAlign = TextAlign.Center,
                                    color = Color(0xFFF8F4E6),
                                    maxLines = 3
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}