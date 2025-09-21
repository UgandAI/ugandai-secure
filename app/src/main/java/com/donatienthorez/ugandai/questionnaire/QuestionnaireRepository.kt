package com.donatienthorez.ugandai.questionnaire

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Step 2: Simple repository for questionnaire data
 * Uses EncryptedSharedPreferences + JSON (reusing existing patterns)
 */
class QuestionnaireRepository(private val context: Context) {
    
    private val encryptedPrefs = getEncryptedSharedPreferences()
    
    private val _questionnaireState = MutableStateFlow(QuestionnaireState())
    val questionnaireState: StateFlow<QuestionnaireState> = _questionnaireState
    
    /**
     * Load user's previous answers (if any)
     */
    fun loadUserProgress() {
        val userId = getCurrentUserId()
        if (userId.isEmpty()) return
        
        val answersJson = encryptedPrefs.getString("questionnaire_answers_$userId", "[]")
        val answers = parseAnswersFromJson(answersJson ?: "[]")
        
        _questionnaireState.value = _questionnaireState.value.copy(
            currentQuestionIndex = answers.size,
            answers = answers,
            isComplete = answers.size >= 5
        )
    }
    
    /**
     * Save a single answer
     */
    fun saveAnswer(answer: QuestionnaireAnswer) {
        val userId = getCurrentUserId()
        if (userId.isEmpty()) return
        
        // Get existing answers
        val answersJson = encryptedPrefs.getString("questionnaire_answers_$userId", "[]")
        val existingAnswers = parseAnswersFromJson(answersJson ?: "[]").toMutableList()
        
        // Remove old answer for same question if exists
        existingAnswers.removeAll { it.questionId == answer.questionId }
        existingAnswers.add(answer)
        
        // Save back to EncryptedSharedPreferences as JSON
        val newAnswersJson = convertAnswersToJson(existingAnswers)
        encryptedPrefs.edit()
            .putString("questionnaire_answers_$userId", newAnswersJson)
            .apply()
        
        // Update state
        val nextIndex = minOf(existingAnswers.size, 5)
        val isComplete = nextIndex >= 5
        
        _questionnaireState.value = _questionnaireState.value.copy(
            currentQuestionIndex = nextIndex,
            answers = existingAnswers,
            isComplete = isComplete
        )
    }
    
    /**
     * Check if questionnaire is complete
     */
    fun isQuestionnaireComplete(): Boolean {
        val userId = getCurrentUserId()
        if (userId.isEmpty()) return false
        
        val answersJson = encryptedPrefs.getString("questionnaire_answers_$userId", "[]")
        val answers = parseAnswersFromJson(answersJson ?: "[]")
        return answers.size >= 5
    }
    
    private fun getCurrentUserId(): String {
        // For now, use a simple approach - later can integrate with existing login
        return encryptedPrefs.getString("user_id", "test_user") ?: "test_user"
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
    
    private fun getEncryptedSharedPreferences(): SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            "questionnaire_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
