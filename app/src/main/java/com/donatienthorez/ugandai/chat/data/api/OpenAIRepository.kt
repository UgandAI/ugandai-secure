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

class OpenAIRepository(private val context: Context) {

    @Throws(NoChoiceAvailableException::class)
    suspend fun sendChatRequest(
        conversation: Conversation,
        userInput: String
    ): Message {

        val token = getTokenFromEncryptedPreferences(context)

        return withContext(Dispatchers.IO) {
            try {
                val url = URL("http://ec2-54-85-226-52.compute-1.amazonaws.com:8000/chats")
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