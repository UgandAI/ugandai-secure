package com.donatienthorez.ugandai.questionnaire

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * simple repository for questionnaire data
 */
class QuestionnaireRepository(private val context: Context) {
    
    private val prefs by lazy {
        context.getSharedPreferences("questionnaire_prefs", Context.MODE_PRIVATE)
    }
    
    private val _questionnaireState = MutableStateFlow(QuestionnaireState())
    val questionnaireState: StateFlow<QuestionnaireState> = _questionnaireState
    
    /**
     * Load user's previous answers (if any)
     */
    fun loadUserProgress() {
        val userId = getCurrentUserId()
        
        val answersJson = prefs.getString("questionnaire_answers_$userId", "[]")
        val answers = parseAnswersFromJson(answersJson ?: "[]")
        
        val nextQuestion = getNextQuestion(answers)
        val isComplete = nextQuestion == null && answers.isNotEmpty()
        
        _questionnaireState.value = _questionnaireState.value.copy(
            currentQuestionIndex = answers.size,
            currentQuestion = nextQuestion,
            answers = answers,
            isComplete = isComplete
        )
    }
    
    /**
     * Save a single answer
     */
    fun saveAnswer(answer: QuestionnaireAnswer) {
        val userId = getCurrentUserId()
        
        // Get existing answers
        val answersJson = prefs.getString("questionnaire_answers_$userId", "[]")
        val existingAnswers = parseAnswersFromJson(answersJson ?: "[]").toMutableList()
        
        // Remove old answer for same question if exists
        existingAnswers.removeAll { it.questionId == answer.questionId }
        existingAnswers.add(answer)
        
        // Save back to SharedPreferences as JSON
        val newAnswersJson = convertAnswersToJson(existingAnswers)
        prefs.edit()
            .putString("questionnaire_answers_$userId", newAnswersJson)
            .apply()
        
        // Determine next question based on current answers
        val nextQuestion = getNextQuestion(existingAnswers)
        val isComplete = nextQuestion == null && existingAnswers.isNotEmpty()
        
        _questionnaireState.value = _questionnaireState.value.copy(
            currentQuestionIndex = existingAnswers.size,
            currentQuestion = nextQuestion,
            answers = existingAnswers,
            isComplete = isComplete
        )
    }
    
    /**
     * Check if questionnaire is complete
     */
    fun isQuestionnaireComplete(): Boolean {
        val userId = getCurrentUserId()
        
        val answersJson = prefs.getString("questionnaire_answers_$userId", "[]")
        val answers = parseAnswersFromJson(answersJson ?: "[]")
        return getNextQuestion(answers) == null && answers.isNotEmpty()
    }
    
    /**
     * Determine next question based on current answers
     */
    private fun getNextQuestion(answers: List<QuestionnaireAnswer>): QuestionnaireQuestion? {
        // If no answers yet, return first question
        if (answers.isEmpty()) {
            return QuestionnaireData.questions.firstOrNull()
        }
        
        // If we have the first answer, find appropriate follow-up
        val firstAnswer = answers.find { it.questionId == "crop_type" }
        if (firstAnswer != null) {
            val firstQuestion = QuestionnaireData.questions.find { it.id == "crop_type" }
            val followUp = firstQuestion?.followUpQuestions?.get(firstAnswer.selectedOption)
            
            // Check if we already answered the follow-up
            val hasFollowUp = answers.any { it.questionId == followUp?.id }
            if (followUp != null && !hasFollowUp) {
                return followUp
            }
        }
        
        // No more questions
        return null
    }
    
    private fun getCurrentUserId(): String {
        // Simple user ID - just use "default_user" for everyone
        return "default_user"
    }
    
    private fun convertAnswersToJson(answers: List<QuestionnaireAnswer>): String {
        val jsonArray = JSONArray()
        answers.forEach { answer ->
            val jsonObject = JSONObject().apply {
                put("questionId", answer.questionId)
                put("selectedOption", answer.selectedOption)
                put("timestamp", answer.timestamp)
            }
            jsonArray.put(jsonObject)
        }
        return jsonArray.toString()
    }
    
    private fun parseAnswersFromJson(jsonString: String): List<QuestionnaireAnswer> {
        val answers = mutableListOf<QuestionnaireAnswer>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                answers.add(
                    QuestionnaireAnswer(
                        questionId = jsonObject.getString("questionId"),
                        selectedOption = jsonObject.getString("selectedOption"),
                        timestamp = jsonObject.getLong("timestamp")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return answers
    }
}