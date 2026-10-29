package com.example.lagallens

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lagallens.data.repository.FakeDraftRepository
import com.example.lagallens.domain.model.DraftStage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DraftRepositoryDeviceTest {
    @Test
    fun legacyDraftMigrationVersioningAndReopenKeepEverySavedContent() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repositoryPreferencesName = "draft_repository_test_migration"
        val legacyPreferencesName = "draft_legacy_test_migration"
        val repositoryPreferences = context.getSharedPreferences(repositoryPreferencesName, Context.MODE_PRIVATE)
        val legacyPreferences = context.getSharedPreferences(legacyPreferencesName, Context.MODE_PRIVATE)
        repositoryPreferences.edit().clear().commit()
        legacyPreferences.edit().clear().commit()
        val oldContent = "HỢP ĐỒNG LAO ĐỘNG\nĐIỀU 1. Nội dung đã lưu trước Phase 3."
        val oldUpdatedAt = 1_760_000_000_000L
        legacyPreferences.edit()
            .putString("draft_d1", "Bản D1 đã lưu")
            .putString("draft_d1_content", oldContent)
            .putString("draft_d1_updated_at", oldUpdatedAt.toString())
            .commit()

        try {
            val repository = FakeDraftRepository(context, repositoryPreferencesName, legacyPreferencesName)
            val migrated = repository.getDraft("D1")
            assertNotNull(migrated)
            assertEquals(oldContent, migrated?.content)
            assertEquals(1, migrated?.versionNo)
            assertEquals(oldContent, legacyPreferences.getString("draft_d1_content", null))

            val firstEdit = repository.saveDraft(
                stage = DraftStage.ORIGINAL,
                title = "Bản D1 đã sửa",
                content = "$oldContent\nĐIỀU 2. Nội dung người dùng nhập.",
                contractId = "contract-from-user-selection",
                sourceDraftId = migrated!!.draftId
            )
            assertNotEquals(migrated.draftId, firstEdit.draftId)
            assertEquals(2, firstEdit.versionNo)
            assertEquals(oldContent, repository.getDraft(migrated.draftId)?.content)

            val repeatedSave = repository.saveDraft(
                DraftStage.ORIGINAL,
                firstEdit.title,
                firstEdit.content,
                sourceDraftId = firstEdit.draftId
            )
            assertEquals(firstEdit.draftId, repeatedSave.draftId)
            assertEquals(firstEdit.versionNo, repeatedSave.versionNo)
            legacyPreferences.edit().remove("draft_d1_content").commit()
            repository.saveDraft(DraftStage.ORIGINAL, firstEdit.title, firstEdit.content, sourceDraftId = firstEdit.draftId)
            assertEquals(firstEdit.content, legacyPreferences.getString("draft_d1_content", null))

            val manual = repository.saveDraft(
                DraftStage.MANUAL_EDIT,
                "Bản M1",
                "HỢP ĐỒNG LAO ĐỘNG\nNội dung người dùng đã chỉnh sửa.",
                sourceDraftId = firstEdit.draftId
            )
            assertEquals(3, manual.versionNo)
            assertEquals("contract-from-user-selection", manual.contractId)

            val copy = repository.createCopy(migrated.draftId, "Bản D3", DraftStage.COPY)
            assertNotNull(copy)
            assertEquals(4, copy?.versionNo)
            assertEquals(oldContent, copy?.content)

            val reopened = FakeDraftRepository(context, repositoryPreferencesName, legacyPreferencesName)
            assertEquals(firstEdit.content, reopened.getDraft("D1")?.content)
            assertEquals(oldContent, reopened.getDraft(migrated.draftId)?.content)
            assertEquals(manual.draftId, reopened.getLatestDraft(DraftStage.MANUAL_EDIT)?.draftId)
            assertEquals(copy?.draftId, reopened.getLatestDraft(DraftStage.COPY)?.draftId)
            assertTrue(reopened.getDrafts().size >= 4)
        } finally {
            repositoryPreferences.edit().clear().commit()
            legacyPreferences.edit().clear().commit()
        }
    }
}
