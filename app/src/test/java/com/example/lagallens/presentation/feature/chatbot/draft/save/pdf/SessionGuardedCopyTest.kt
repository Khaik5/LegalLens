package com.example.lagallens.presentation.feature.chatbot.draft.save.pdf

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SessionGuardedCopyTest {
    @Test fun sessionChangeStopsFurtherWritesButCannotUndoPublishedBytes() {
        var session = "a:1"
        val output = object : ByteArrayOutputStream() {
            override fun write(bytes: ByteArray, offset: Int, length: Int) {
                super.write(bytes, offset, length)
                session = "b:2"
            }
        }
        val source = ByteArray(17000) { 7 }
        assertThrows(IllegalStateException::class.java) {
            copyWithSessionGuard(ByteArrayInputStream(source), output) { action ->
                check(session == "a:1")
                action()
            }
        }
        assertEquals(8192, output.size())
    }

    @Test fun sameUserNewGenerationCannotWrite() {
        val output = ByteArrayOutputStream()
        assertThrows(IllegalStateException::class.java) {
            copyWithSessionGuard(ByteArrayInputStream(byteArrayOf(1)), output) { action ->
                check("a:2" == "a:1")
                action()
            }
        }
        assertEquals(0, output.size())
    }
}
