package com.example.lagallens.presentation.feature.analysis

import com.example.lagallens.data.mapper.snapshotTextForLanguage
import org.junit.Assert.*
import org.junit.Test

class SnapshotTextTest {
    @Test fun legacyEnglishIsUnavailableWithoutChangingStoredVietnamese() {
        val snapshot = mapOf("vi:title" to "Hợp đồng", "en:title" to "Hợp đồng", "identity:resultId" to "result-a")
        val original = snapshot.toMap()
        assertNull(snapshotTextForLanguage(snapshot, "title", "en"))
        assertEquals("Hợp đồng", snapshotTextForLanguage(snapshot, "title", "vi"))
        assertEquals(original, snapshot)
    }

    @Test fun markedSnapshotReadsItsOwnTranslationAndNeverGuesses() {
        val first = mapOf("translation:en" to "complete-v1", "vi:title" to "A", "en:title" to "First")
        val second = mapOf("translation:en" to "complete-v1", "vi:title" to "B", "en:title" to "Second")
        assertEquals("First", snapshotTextForLanguage(first, "title", "en"))
        assertEquals("Second", snapshotTextForLanguage(second, "title", "en"))
        assertNull(snapshotTextForLanguage(first, "missing", "en"))
        assertNull(snapshotTextForLanguage(first + ("translation:en" to "unknown"), "title", "en"))
    }
}
