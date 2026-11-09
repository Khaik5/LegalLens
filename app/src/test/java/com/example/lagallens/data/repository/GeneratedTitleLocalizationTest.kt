package com.example.lagallens.data.repository

import com.example.lagallens.data.datasource.local.datastore.EmploymentDemoDataSource
import com.example.lagallens.domain.model.DraftStage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class GeneratedTitleLocalizationTest {
    @Test fun generatedContractTitlesFollowLanguageButExplicitRenamesNeverDo() {
        val prefs = EmploymentPersistenceTest().preferences()
        var language = "vi"
        val store = EmploymentDemoDataSource(prefs) { "$language:$it" }
        val original = checkNotNull(store.getContract("collaborator"))
        language = "en"
        assertTrue(checkNotNull(store.getContract(original.id)).title.startsWith("en:"))
        assertEquals(original.content, store.getContract(original.id)?.content)
        val renamed = checkNotNull(store.getContract(original.id)).title
        assertTrue(store.renameContract(original.id, renamed))
        language = "vi"
        assertEquals(renamed, store.getContract(original.id)?.title)
        assertEquals(renamed, EmploymentDemoDataSource(prefs) { "new:$it" }.getContract(original.id)?.title)
        assertTrue(store.localizedContracts().first { it.id == "office-lease" }.title.startsWith("vi:"))
    }

    @Test fun generatedDraftTitlesFollowLocaleWithoutChangingUserNamesOrContent() = runBlocking {
        val prefs = EmploymentPersistenceTest().preferences()
        val legacy = EmploymentPersistenceTest().preferences()
        var language = "vi"
        val repository = FakeDraftRepository(prefs, legacy, generatedTitle = { "$language:${it.legacyKey}" })
        val generated = repository.saveDraft(DraftStage.ORIGINAL, "vi:D1", "edited body", isTitleGenerated = true)
        val custom = repository.saveDraft(DraftStage.MANUAL_EDIT, "vi:M1", "user text")
        language = "en"
        assertEquals("en:D1", repository.getDraft(generated.draftId)?.title)
        assertEquals("edited body", repository.getDraft(generated.draftId)?.content)
        assertEquals("vi:M1", repository.getDraft(custom.draftId)?.title)
        val renamed = checkNotNull(repository.updateDraft(generated.draftId, "en:D1", "changed"))
        language = "vi"
        assertEquals("en:D1", repository.getDraft(renamed.draftId)?.title)
        val reopened = FakeDraftRepository(prefs, legacy, generatedTitle = { "en:${it.legacyKey}" })
        assertEquals("en:D1", reopened.getDraft(generated.draftId)?.title)
        assertEquals("vi:M1", reopened.getDraft(custom.draftId)?.title)
    }
}
