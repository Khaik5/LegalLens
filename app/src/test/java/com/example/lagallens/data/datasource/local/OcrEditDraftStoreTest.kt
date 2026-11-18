package com.example.lagallens.data.datasource.local

import java.io.File
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrEditDraftStoreTest {
    private val cache: File = Files.createTempDirectory("ocr-drafts").toFile()
    private val store = OcrEditDraftStore(cache)

    @After fun cleanUp() { cache.deleteRecursively() }

    @Test fun aSavedDraftComesBackWithBothTexts() {
        val draft = OcrEditDraftStore.Draft("Bản gốc\nĐiều 1", "Bản sửa\nĐiều 1 (đã sửa)")

        assertTrue(store.save(OcrEditDraftStore.PURPOSE_EDITOR, "session-a", "41", draft))

        assertEquals(draft, store.load(OcrEditDraftStore.PURPOSE_EDITOR, "session-a", "41"))
    }

    @Test fun savingAgainReplacesTheEarlierDraft() {
        store.save("editor", "s", "41", OcrEditDraftStore.Draft("a", "b"))
        store.save("editor", "s", "41", OcrEditDraftStore.Draft("a", "c"))

        assertEquals("c", store.load("editor", "s", "41")?.edited)
        assertEquals(1, File(cache, OcrEditDraftStore.DIRECTORY).listFiles()!!.size)
    }

    @Test fun aDraftIsOnlyReturnedToTheSessionDocumentAndPurposeThatWroteIt() {
        store.save("editor", "session-a", "41", OcrEditDraftStore.Draft("a", "b"))

        assertNull(store.load("editor", "session-b", "41"))
        assertNull(store.load("editor", "session-a", "42"))
        assertNull(store.load("review", "session-a", "41"))
    }

    @Test fun clearingRemovesTheDraft() {
        store.save("editor", "s", "41", OcrEditDraftStore.Draft("a", "b"))

        store.clear("editor", "s", "41")

        assertNull(store.load("editor", "s", "41"))
    }

    @Test fun nothingReadableIsLeftInTheFileNames() {
        store.save("editor", "session-secret", "document-777", OcrEditDraftStore.Draft("a", "b"))

        val names = File(cache, OcrEditDraftStore.DIRECTORY).list()!!.joinToString()

        assertFalse(names.contains("session-secret"))
        assertFalse(names.contains("777"))
    }

    @Test fun aDamagedFileIsIgnoredInsteadOfCrashing() {
        store.save("editor", "s", "41", OcrEditDraftStore.Draft("a", "b"))
        File(cache, OcrEditDraftStore.DIRECTORY).listFiles()!!.single().writeText("{ not json")

        assertNull(store.load("editor", "s", "41"))
    }

    @Test fun theSessionCleanerRemovesDraftsOfTheEndedSession() {
        store.save("editor", "s", "41", OcrEditDraftStore.Draft("a", "b"))
        File(cache, OcrEditDraftStore.DIRECTORY).listFiles()!!.single().setLastModified(1_000)

        SessionCacheCleaner(cache).clear(createdBefore = 2_000)

        assertNull(store.load("editor", "s", "41"))
    }
}
