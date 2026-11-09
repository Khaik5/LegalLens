package com.example.lagallens.presentation.feature.chatbot.draft.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class DraftSessionGuardTest {
    @Test fun restoredIntentRetainsOriginalGeneration() {
        val savedIntentOwner = "a:1"
        assertTrue(DraftSessionGuard.matches(savedIntentOwner, "a:1"))
        assertFalse(DraftSessionGuard.matches(savedIntentOwner, null))
        assertFalse(DraftSessionGuard.matches(savedIntentOwner, "b:2"))
        assertFalse(DraftSessionGuard.matches(savedIntentOwner, "a:3"))
        assertFalse(DraftSessionGuard.matches(null, null))
    }
    @Test fun staleParentCannotStampNewAccountOnChildIntent() {
        assertEquals("a:1", DraftSessionGuard.propagatedOwner(true, "a:1", "b:2"))
        assertEquals("a:1", DraftSessionGuard.propagatedOwner(true, "a:1", "a:3"))
        assertEquals(null, DraftSessionGuard.propagatedOwner(true, null, "b:2"))
        assertEquals("b:2", DraftSessionGuard.propagatedOwner(false, null, "b:2"))
    }
}
