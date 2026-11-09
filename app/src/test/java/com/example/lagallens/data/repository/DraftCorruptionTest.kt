package com.example.lagallens.data.repository

import com.example.lagallens.domain.model.DraftStage
import com.example.lagallens.data.datasource.local.datastore.DemoSessionScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class DraftCorruptionTest {
    @Test fun generationChangeBeforeCommitDoesNotWriteEitherStore() = runBlocking {
        val prefs = EmploymentPersistenceTest().preferences()
        val mirror = EmploymentPersistenceTest().preferences()
        prefs.edit().putBoolean("legacy_saved_drafts_migrated", true).putString("draft_ids", "[]").commit()
        val original = DemoSessionScope("account-a", "generation-1")
        var current = original
        val repo = FakeDraftRepository(prefs, mirror) { action ->
            current = DemoSessionScope("account-a", "generation-2")
            check(current == original) { "Session changed" }
            action()
        }
        assertTrue(runCatching { repo.saveDraft(DraftStage.ORIGINAL, "title", "private") }.isFailure)
        assertEquals("[]", prefs.getString("draft_ids", null))
        assertFalse(mirror.contains("draft_d1"))
    }
    @Test fun malformedIndexIsNotReplacedDuringSave() = runBlocking {
        val prefs = EmploymentPersistenceTest().preferences()
        val legacy = EmploymentPersistenceTest().preferences()
        prefs.edit().putBoolean("legacy_saved_drafts_migrated", true).putString("draft_ids", "bad-json").commit()
        val repository = FakeDraftRepository(prefs, legacy)
        assertTrue(runCatching { repository.saveDraft(DraftStage.ORIGINAL, "title", "content") }.isFailure)
        assertEquals("bad-json", prefs.getString("draft_ids", null))
    }

    @Test fun missingSnapshotCannotBeSilentlyDiscarded() = runBlocking {
        val prefs = EmploymentPersistenceTest().preferences()
        val legacy = EmploymentPersistenceTest().preferences()
        prefs.edit().putBoolean("legacy_saved_drafts_migrated", true).putString("draft_ids", "[\"missing\"]").commit()
        val repository = FakeDraftRepository(prefs, legacy)
        assertTrue(runCatching { repository.saveDraft(DraftStage.ORIGINAL, "title", "content") }.isFailure)
        assertEquals("[\"missing\"]", prefs.getString("draft_ids", null))
    }
}
