package com.donatienthorez.ugandai.questionnaire

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.donatienthorez.ugandai.chat.ui.ChatActivity
import com.ugandai.ugandai.ui.ChatGptBotAppTheme

/**
 * Questionnaire activity for 2-question flow
 */
class QuestionnaireActivity : ComponentActivity() {
    
    private lateinit var repository: QuestionnaireRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        repository = QuestionnaireRepository(this)
        repository.loadUserProgress()
        
        setContent {
            ChatGptBotAppTheme {
                QuestionnaireScreen(
                    repository = repository,
                    onComplete = { 
                        // Navigate to chat when done
                        val intent = Intent(this@QuestionnaireActivity, ChatActivity::class.java)
                        startActivity(intent)
                        finish()
                    },
                    onSkip = {
                        // Skip to chat
                        val intent = Intent(this@QuestionnaireActivity, ChatActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionnaireScreen(
    repository: QuestionnaireRepository,
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val state = repository.questionnaireState.collectAsState()
    
    // Auto-complete when questionnaire is done
    LaunchedEffect(state.value.isComplete) {
        if (state.value.isComplete) {
            onComplete()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Text(
            text = "Quick Questions",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Text(
            text = "Help us understand your farming needs",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Progress
        LinearProgressIndicator(
            progress = state.value.currentQuestionIndex.toFloat() / 2f,
            modifier = Modifier.fillMaxWidth(),
        )
        
        Text(
            text = "Question ${state.value.currentQuestionIndex + 1}",
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Current question - use dynamic question from state
        state.value.currentQuestion?.let { question ->
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = question.text,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Options as selectable cards
                    question.options.forEach { option ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .selectable(
                                    selected = false,
                                    onClick = {
                                        val answer = QuestionnaireAnswer(
                                            questionId = question.id,
                                            selectedOption = option
                                        )
                                        repository.saveAnswer(answer)
                                    }
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = false,
                                    onClick = null
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Skip button
        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Skip for now")
        }
    }
}
