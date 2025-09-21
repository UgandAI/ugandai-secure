package com.donatienthorez.ugandai.questionnaire

/**
 * Basic data models for questionnaire
 */

data class QuestionnaireQuestion(
    val id: String,
    val text: String,
    val options: List<String>
)

data class QuestionnaireAnswer(
    val questionId: String,
    val selectedOption: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class QuestionnaireState(
    val currentQuestionIndex: Int = 0,
    val answers: List<QuestionnaireAnswer> = emptyList(),
    val isComplete: Boolean = false,
    val isLoading: Boolean = false
)

/**
 * Sample questions
 */
object QuestionnaireData {
    val STARTER_QUESTIONS = listOf(
        QuestionnaireQuestion(
            id = "crop",
            text = "What crop are you primarily growing?",
            options = listOf("Maize (Corn)", "Beans", "Coffee", "Bananas", "Cassava", "Sweet Potatoes", "Other")
        ),
        QuestionnaireQuestion(
            id = "challenge", 
            text = "What is your biggest farming challenge?",
            options = listOf("Pests & Diseases", "Weather/Climate", "Poor Soil", "Lack of Water", "Limited Seeds/Tools", "Market Access", "Knowledge Gap")
        ),
        QuestionnaireQuestion(
            id = "land_size",
            text = "How much land do you farm?",
            options = listOf("Less than 1 acre", "1-2 acres", "3-5 acres", "6-10 acres", "More than 10 acres")
        ),
        QuestionnaireQuestion(
            id = "location",
            text = "Which region of Uganda are you in?",
            options = listOf("Central", "Eastern", "Northern", "Western", "Prefer not to say")
        ),
        QuestionnaireQuestion(
            id = "goal",
            text = "What is your main farming goal?",
            options = listOf("Feed my family", "Sell for income", "Both food and income", "Learn new techniques", "Expand my farm")
        )
    )
}
