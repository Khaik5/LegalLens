package com.example.lagallens.core.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ReadableTextTest {
    @Test fun `no-break spaces become plain spaces`() =
        assertEquals("Ông Nguyễn Văn An", "Ông Nguyễn Văn An".toReadableText())

    @Test fun `plain text and line breaks are kept`() =
        assertEquals("Điều 1.\nThời hạn", "Điều 1.\nThời hạn".toReadableText())
}
