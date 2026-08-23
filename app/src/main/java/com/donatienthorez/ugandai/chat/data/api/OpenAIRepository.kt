package com.donatienthorez.ugandai.chat.data.api

import android.content.Context
import com.ugandai.ugandai.chat.data.Conversation
import com.ugandai.ugandai.utils.NetworkConfig
import com.ugandai.ugandai.auth.data.AuthTokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class Citation(
    val documentId: Int,
    val title: String,
    val source: String,
    val url: String?,
    val chunkId: Int,
    val chunkIndex: Int
)

data class VoiceChatResult(
    val transcript: String,
    val content: String,
    val citations: List<Citation>,
    val audioBytes: ByteArray,
    val audioFormat: String
)

internal fun parseCitationsArray(json: JSONObject): List<Citation> {
    val values = json.optJSONArray("citations") ?: return emptyList()
    return (0 until values.length()).map { index ->
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
}

sealed class ChatStreamEvent {
    data class Content(val text: String) : ChatStreamEvent()
    data class Citations(val items: List<Citation>) : ChatStreamEvent()
}

fun parseChatStreamEvent(data: String): ChatStreamEvent? {
    val json = JSONObject(data)
    if (json.has("content")) return ChatStreamEvent.Content(json.getString("content"))
    if (json.has("citations")) return ChatStreamEvent.Citations(parseCitationsArray(json))
    return null
}

class OpenAIRepository(private val context: Context) {

    private val tokenStore = AuthTokenStore(context)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // SSE needs no timeout
        .build()

    fun sendChatRequestStream(userInput: String): Flow<ChatStreamEvent> = callbackFlow {
        val token = tokenStore.token()
        
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
                    close(IOException("Invalid chat stream response", e))
                }
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                val message = when (response?.code) {
                    401 -> "Authentication expired"
                    403 -> "Chat access forbidden"
                    else -> "Chat stream disconnected"
                }
                close(IOException(message, t))
            }
        })

        awaitClose {
            eventSource.cancel()
        }
    }

    /**
     * Speech in, speech out. Backend transcribes [audioBytes], runs the transcript through
     * the same RAG chat pipeline as [sendChatRequestStream], and returns a synthesized reply.
     * Matches POST /voice/chat on the Web-Server backend.
     */
    suspend fun sendVoiceChat(audioBytes: ByteArray, filename: String = "voice.m4a"): VoiceChatResult =
        withContext(Dispatchers.IO) {
            require(audioBytes.isNotEmpty()) { "Voice recording is empty" }
            val token = tokenStore.token()

            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "audio", filename,
                    audioBytes.toRequestBody("audio/mp4".toMediaType())
                )
                .build()

            val requestBuilder = Request.Builder()
                .url("${NetworkConfig.BASE_URL}/voice/chat")
                .post(multipartBody)

            if (token != null) {
                requestBuilder.header("Authorization", "Bearer $token")
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                val bodyText = response.body?.string()
                if (!response.isSuccessful || bodyText == null) {
                    throw IOException(when (response.code) {
                        401 -> "Authentication expired"
                        403 -> "Voice chat access forbidden"
                        413 -> "Voice recording is too large"
                        415, 422 -> "Voice recording is invalid"
                        else -> "Voice chat failed (${response.code})"
                    })
                }
                val json = JSONObject(bodyText)
                VoiceChatResult(
                    transcript = json.getString("transcript"),
                    content = json.getString("content"),
                    citations = parseCitationsArray(json),
                    audioBytes = android.util.Base64.decode(json.getString("audio_base64"), android.util.Base64.DEFAULT),
                    audioFormat = json.optString("audio_format", "mp3")
                )
            }
        }

}
