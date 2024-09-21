package com.ugandai.chatgptbot.chat.data.api

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.client.OpenAI
import com.ugandai.chatgptbot.chat.data.Conversation
import com.ugandai.chatgptbot.chat.data.Message
import com.ugandai.chatgptbot.chat.data.MessageStatus
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

@OptIn(BetaOpenAI::class)
class OpenAIRepository(private val openAI: OpenAI, private val context: Context) {

    @Throws(NoChoiceAvailableException::class)
    suspend fun sendChatRequest(
        conversation: Conversation,
        userInput: String // Dynamic user input
    ): Message {
        val instructions = "- You are an assistant helping farmers in rural Uganda make better decisions about planting crops\n" +
                "- Only refer to the information provided in the files; crops.json, buyangaWeather.json, mbaleWeather.json, namutumbaWeather.json\n"

        var contentString: String

        // Retrieve token from encrypted shared preferences
        val token = getTokenFromEncryptedPreferences(context)

        // Execute network operation on IO thread
        contentString = withContext(Dispatchers.IO) {
            try {
                val url = URL("http://127.0.0.1:8000/chats")
                val con = url.openConnection() as HttpURLConnection

                con.requestMethod = "POST"
                con.setRequestProperty("Content-Type", "application/json; utf-8")
                con.setRequestProperty("Accept", "application/json")

                // Include token in the request header if it's not null
                token?.let {
                    con.setRequestProperty("Authorization", "Bearer $it")
                }

                con.doOutput = true

                // Create the JSON input string with dynamic user input
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
                    jsonObject.getString("content") // Extract content from JSON response
                }
            } catch (e: Exception) {
                e.printStackTrace()
                "Error: ${e.message}" // Return error message in case of failure
            }
        }

        return Message(
            text = contentString,
            isFromUser = false, // Set this to false to indicate it's from the AI
            messageStatus = MessageStatus.Sent
        )
    }

    private fun Conversation.toChatMessages() = this.list
        .filterNot { it.messageStatus == MessageStatus.Error }
        .map {
            ChatMessage(
                content = it.text,
                role = if (it.isFromUser) ChatRole.User else ChatRole.Assistant
            )
        }

    // Method to retrieve token from Encrypted SharedPreferences
    private fun getTokenFromEncryptedPreferences(context: Context): String? {
        return try {
            // Get the master key for encryption
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

            // Create (or retrieve) EncryptedSharedPreferences object
            val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
                "secure_prefs",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            // Retrieve the token securely
            sharedPreferences.getString("user_token", null)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

// Custom exception class for handling specific errors
class NoChoiceAvailableException : Exception()
