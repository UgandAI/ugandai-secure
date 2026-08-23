package com.donatienthorez.ugandai

import com.donatienthorez.ugandai.chat.data.api.ChatStreamEvent
import com.donatienthorez.ugandai.chat.data.api.parseChatStreamEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatCitationParsingTest {
    @Test
    fun contentEventParsesWithoutCitations() {
        val event = parseChatStreamEvent("""{"content":"Plant after rain."}""")
        assertEquals(ChatStreamEvent.Content("Plant after rain."), event)
        assertNull(parseChatStreamEvent("""{"done":true}"""))
    }

    @Test
    fun citationsParseWithOptionalUrl() {
        val event = parseChatStreamEvent(
            """{"citations":[{"document_id":7,"title":"Maize notes","source":"fixture:maize","url":null,"chunk_id":11,"chunk_index":0}]}"""
        ) as ChatStreamEvent.Citations
        assertEquals(1, event.items.size)
        assertEquals(7, event.items.first().documentId)
        assertEquals("Maize notes", event.items.first().title)
        assertNull(event.items.first().url)
    }
}
