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
    val selectedOption: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class QuestionnaireState(
    val currentQuestionIndex: Int = 0,
    val currentQuestion: QuestionnaireQuestion? = null,
    val answers: List<QuestionnaireAnswer> = emptyList(),
    val isComplete: Boolean = false
)

/**
 * Sample questions
 */
object QuestionnaireData {
    val questions = listOf(
        QuestionnaireQuestion(
            id = "crop_type",
            text = "What is your primary crop?",
            options = listOf("Maize", "Coffee", "Beans", "Bananas"),
            followUpQuestions = mapOf(
                "Maize" to QuestionnaireQuestion(
                    id = "maize_challenge",
                    text = "What is your biggest challenge with maize farming?",
                    options = listOf("Pests", "Weather", "Seeds quality", "Market prices")
                ),
                "Coffee" to QuestionnaireQuestion(
                    id = "coffee_challenge", 
                    text = "What is your main coffee farming concern?",
                    options = listOf("Disease", "Processing", "Quality", "Buyers")
                ),
                "Beans" to QuestionnaireQuestion(
                    id = "beans_challenge",
                    text = "What issue do you face with beans?",
                    options = listOf("Soil fertility", "Storage", "Harvesting", "Varieties")
                ),
                "Bananas" to QuestionnaireQuestion(
                    id = "bananas_challenge",
                    text = "What is your main banana farming challenge?",
                    options = listOf("Diseases", "Spacing", "Fertilizers", "Harvesting")
                )
            )
        )
    )
}
