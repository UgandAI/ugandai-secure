package com.donatienthorez.ugandai

import com.donatienthorez.ugandai.chat.data.api.parseCitationsArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceChatParsingTest {
    @Test
    fun voiceChatResponseCitationsParseWithOptionalUrl() {
        val json = JSONObject(
            """
            {
              "transcript": "When should I plant maize?",
              "content": "Plant maize after the rains begin.",
              "citations": [{"document_id":7,"title":"Maize notes","source":"fixture:maize","url":null,"chunk_id":11,"chunk_index":0,"score":0.91}],
              "audio_base64": "ZmFrZS1tcDMtYnl0ZXM=",
              "audio_format": "mp3"
            }
            """.trimIndent()
        )
        val citations = parseCitationsArray(json)
        assertEquals(1, citations.size)
        assertEquals(7, citations.first().documentId)
        assertEquals("Maize notes", citations.first().title)
        assertNull(citations.first().url)
    }

    @Test
    fun voiceChatResponseWithoutCitationsParsesEmptyList() {
        val json = JSONObject(
            """{"transcript":"hello","content":"hi there","audio_base64":"ZmFrZQ==","audio_format":"mp3"}"""
        )
        assertTrue(parseCitationsArray(json).isEmpty())
    }
}
