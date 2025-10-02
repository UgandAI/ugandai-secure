package com.donatienthorez.ugandai.questionnaire

/**
 * Basic data models for questionnaire
 */

data class QuestionnaireQuestion(
    val id: String,
    val text: String,
    val options: List<String>,
    val followUpQuestions: Map<String, QuestionnaireQuestion> = emptyMap() // Maps option -> follow-up question
)

data class QuestionnaireAnswer(
    val questionId: String,
    val selectedOption: String
)

data class QuestionnaireState(
    val currentQuestion: QuestionnaireQuestion? = null,
    val answers: List<QuestionnaireAnswer> = emptyList(),
    val isComplete: Boolean = false
)

/**
 * Simple 2-question flow
 */
object QuestionnaireData {
    val cropQuestion = QuestionnaireQuestion(
        id = "crop_type",
        text = "What is your primary crop?",
        options = listOf("Maize", "Coffee", "Beans", "Bananas")
    )
    
    val challengeQuestion = QuestionnaireQuestion(
        id = "challenge",
        text = "What is your biggest farming challenge?",
        options = listOf("Pests", "Weather", "Soil", "Market")
    )
}
