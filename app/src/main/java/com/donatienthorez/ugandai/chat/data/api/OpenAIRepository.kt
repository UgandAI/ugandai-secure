package com.ugandai.ugandai.chat.data.api

import com.ugandai.ugandai.chat.data.Conversation
import com.ugandai.ugandai.chat.data.Message
import com.ugandai.ugandai.chat.data.MessageStatus
import com.donatienthorez.ugandai.chat.data.api.ProposedActivity
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.ugandai.ugandai.utils.NetworkConfig

class OpenAIRepository(private val context: Context) {

    @Throws(NoChoiceAvailableException::class)
    suspend fun sendChatRequest(
        conversation: Conversation,
        userInput: String
    ): Message {
        if (NetworkConfig.USE_MOCK_SERVER) {
            kotlinx.coroutines.delay(1000) // Simulate network latency

            val lowerInput = userInput.lowercase()
            val proposed = when {
                lowerInput.contains("plant") || lowerInput.contains("sow") -> {
                    ProposedActivity(
                        activityType = "PLANTED",
                        crop = if (lowerInput.contains("maize")) "maize" else if (lowerInput.contains("bean")) "beans" else "coffee",
                        date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
                        note = "Planted crop via mock assistant suggestion",
                        confidence = 0.95
                    )
                }
                lowerInput.contains("weed") -> {
                    ProposedActivity(
                        activityType = "WEEDED",
                        crop = "maize",
                        date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
                        note = "Weeded fields",
                        confidence = 0.90
                    )
                }
                lowerInput.contains("fertilize") -> {
                    ProposedActivity(
                        activityType = "FERTILIZED",
                        crop = "maize",
                        date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
                        note = "Fertilized soil",
                        confidence = 0.92
                    )
                }
                lowerInput.contains("spray") -> {
                    ProposedActivity(
                        activityType = "SPRAYED",
                        crop = "beans",
                        date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
                        note = "Sprayed crop with organic pest control",
                        confidence = 0.88
                    )
                }
                lowerInput.contains("harvest") -> {
                    ProposedActivity(
                        activityType = "HARVESTED",
                        crop = "maize",
                        date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
                        note = "Harvested yield",
                        confidence = 0.97
                    )
                }
                lowerInput.contains("water") -> {
                    ProposedActivity(
                        activityType = "WATERED",
                        crop = "coffee",
                        date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
                        note = "Irrigated field",
                        confidence = 0.94
                    )
                }
                else -> null
            }

            val responseText = if (proposed != null) {
                "I detected that you are performing a farm activity. I suggest logging it: **${proposed.activityType.lowercase().replaceFirstChar { it.uppercase() }}** **${proposed.crop}** on **${proposed.date}**. You can add it to your logbook below!"
            } else {
                "Hello farmer! I am your AI assistant running in mock mode. You can ask me about planting, weeding, fertilizing, spraying, watering, or harvesting crops to test the logbook entry suggestions."
            }

            return Message(
                text = responseText,
                isFromUser = false,
                messageStatus = MessageStatus.Sent,
                proposedActivity = proposed
            )
        }

        val token = getTokenFromEncryptedPreferences(context)

        return withContext(Dispatchers.IO) {
            try {
                val url = URL("${NetworkConfig.BASE_URL}/chats")
                val con = url.openConnection() as HttpURLConnection

                con.requestMethod = "POST"
                con.setRequestProperty("Content-Type", "application/json; utf-8")
                con.setRequestProperty("Accept", "application/json")

                token?.let {
                    con.setRequestProperty("Authorization", "Bearer $it")
                }

                con.doOutput = true

                val jsonInputString = """{"sender": "user", "content": "$userInput"}"""
                DataOutputStream(con.outputStream).use { out ->
                    out.writeBytes(jsonInputString)
                    out.flush()
                }

                BufferedReader(InputStreamReader(con.inputStream, StandardCharsets.UTF_8)).use { reader ->
                    val content = StringBuilder()
                    var inputLine: String?
                    while (reader.readLine().also { inputLine = it } != null) {
                        content.append(inputLine)
                    }

                    val responseString = content.toString()
                    val jsonObject = JSONObject(responseString)

                    val contentText = jsonObject.getString("content")

                    var proposedActivity: ProposedActivity? = null

                    if (jsonObject.has("proposed_activity") && !jsonObject.isNull("proposed_activity")) {
                        val activityObj = jsonObject.getJSONObject("proposed_activity")

                        proposedActivity = ProposedActivity(
                            activityType = activityObj.getString("activity_type"),
                            crop = activityObj.getString("crop"),
                            date = activityObj.getString("date"),
                            note = activityObj.optString("note", null),
                            confidence = activityObj.getDouble("confidence")
                        )
                    }

                    Message(
                        text = contentText,
                        isFromUser = false,
                        messageStatus = MessageStatus.Sent,
                        proposedActivity = proposedActivity
                    )
                }

            } catch (e: Exception) {
                e.printStackTrace()

                Message(
                    text = "Error: ${e.message}",
                    isFromUser = false,
                    messageStatus = MessageStatus.Error,
                    proposedActivity = null
                )
            }
        }
    }

    private fun getTokenFromEncryptedPreferences(context: Context): String? {
        return try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

            val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
                "secure_prefs",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            sharedPreferences.getString("user_token", null)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

class NoChoiceAvailableException : Exception()