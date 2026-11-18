package com.example.lagallens.presentation.feature.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OcrTextHandoffTest {
    @Test
    fun peekKeepsTheTextForRecreationAndReleaseRemovesIt() {
        val token = OcrTextHandoff.put("Nội dung hợp đồng")

        assertEquals("Nội dung hợp đồng", OcrTextHandoff.peek(token))
        assertEquals("Nội dung hợp đồng", OcrTextHandoff.peek(token))
        OcrTextHandoff.release(token)

        assertNull(OcrTextHandoff.peek(token))
    }

    @Test
    fun takeReturnsTheTextOnlyOnce() {
        val token = OcrTextHandoff.put("Kết quả chỉnh sửa")

        assertEquals("Kết quả chỉnh sửa", OcrTextHandoff.take(token))
        assertNull(OcrTextHandoff.take(token))
    }

    @Test
    fun missingOrNullTokenGivesNoText() {
        assertNull(OcrTextHandoff.peek(null))
        assertNull(OcrTextHandoff.peek("không-tồn-tại"))
        assertNull(OcrTextHandoff.take(null))
        OcrTextHandoff.release(null)
    }

    @Test
    fun anEmptyTextIsStillAValidHandoff() {
        val token = OcrTextHandoff.put("")

        assertEquals("", OcrTextHandoff.peek(token))
        OcrTextHandoff.release(token)
    }

    @Test
    fun oldestEntriesAreDroppedWhenScreensNeverCollectThem() {
        val first = OcrTextHandoff.put("đầu tiên")
        val tokens = (1..10).map { OcrTextHandoff.put("văn bản $it") }

        assertNull(OcrTextHandoff.peek(first))
        assertEquals("văn bản 10", OcrTextHandoff.peek(tokens.last()))
        assertNotEquals(tokens.first(), tokens.last())
        tokens.forEach(OcrTextHandoff::release)
    }
}
