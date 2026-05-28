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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.CheckCircle
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
            "What crops should I plant this week based on the current weather in my area?",
            Icons.Default.WbSunny
        ),
        PresetPrompt(
            "I'm planting maize next week, how do I prepare my land step by step?",
            Icons.Default.Agriculture
        ),
        PresetPrompt(
            "My coffee leaves are turning yellow, what is causing it and how can I fix it?",
            Icons.Default.LocalHospital
        ),
        PresetPrompt(
            "When should I plant, fertilize, and harvest maize this season?",
            Icons.Default.DateRange
        ),
        PresetPrompt(
            "I have 2 acres and limited rainfall, what crops will give me the best yield?",
            Icons.Default.WaterDrop
        ),
        PresetPrompt(
            "What farm activities should I be doing this month for my crops?",
            Icons.Default.CheckCircle
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
            containerColor = Color.Transparent
        ) { paddingValues ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 20.dp)
                ) {
                    Text(
                        text = "Get Started with Some Examples:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        letterSpacing = 0.5.sp,
                        color = Color(0xFFF8F4E6),
                        lineHeight = 28.sp
                    )
                }

                // Prompts Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    userScrollEnabled = false
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
                            .height(220.dp)
                            .scale(scale)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                onPromptSelected(prompt.text)
                            },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.08f)
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.08f),
                                            Color.Transparent
                                        ),
                                        radius = 700f
                                    )
                                )
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {

                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {

                                Icon(
                                    imageVector = prompt.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(42.dp),
                                    tint = Color(0xFFE0C07A)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = prompt.text,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    lineHeight = 18.sp,
                                    letterSpacing = 0.3.sp,
                                    textAlign = TextAlign.Center,
                                    color = Color(0xFFF8F4E6),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }     // Close Column
    }       // Close Scaffold
}         // Close Box
}         // Close fun PresetPromptsScreen