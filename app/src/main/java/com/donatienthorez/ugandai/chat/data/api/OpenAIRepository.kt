package com.donatienthorez.ugandai.chat.data.api

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.ugandai.ugandai.chat.data.Conversation
import com.ugandai.ugandai.utils.NetworkConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class Citation(
    val documentId: Int,
    val title: String,
    val source: String,
    val url: String?,
    val chunkId: Int,
    val chunkIndex: Int
)

sealed class ChatStreamEvent {
    data class Content(val text: String) : ChatStreamEvent()
    data class Citations(val items: List<Citation>) : ChatStreamEvent()
}

fun parseChatStreamEvent(data: String): ChatStreamEvent? {
    val json = JSONObject(data)
    if (json.has("content")) return ChatStreamEvent.Content(json.getString("content"))
    if (json.has("citations")) {
        val values = json.getJSONArray("citations")
        val citations = (0 until values.length()).map { index ->
            val item = values.getJSONObject(index)
            Citation(
                documentId = item.getInt("document_id"),
                title = item.getString("title"),
                source = item.getString("source"),
                url = item.optString("url").takeUnless { it.isBlank() || it == "null" },
                chunkId = item.getInt("chunk_id"),
                chunkIndex = item.getInt("chunk_index")
            )
        }
        return ChatStreamEvent.Citations(citations)
    }
    return null
}

class OpenAIRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // SSE needs no timeout
        .build()

    fun sendChatRequestStream(userInput: String): Flow<ChatStreamEvent> = callbackFlow {
        val token = getTokenFromEncryptedPreferences(context)
        
        val jsonInput = JSONObject().apply {
            put("sender", "user")
            put("content", userInput)
        }.toString()

        val requestBuilder = Request.Builder()
            .url("${NetworkConfig.BASE_URL}/chats")
            .post(jsonInput.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .header("Accept", "text/event-stream")

        if (token != null) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        val request = requestBuilder.build()
        val factory = EventSources.createFactory(client)

        val eventSource = factory.newEventSource(request, object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                try {
                    val json = JSONObject(data)
                    if (json.has("error")) {
                        close(Exception(json.getString("error")))
                    } else {
                        parseChatStreamEvent(data)?.let { trySend(it) }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                close(t ?: Exception("SSE Stream Failed"))
            }
        })

        awaitClose {
            eventSource.cancel()
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
            null
        }
    }
}
