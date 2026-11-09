package com.example.lagallens

import android.os.SystemClock
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.core.content.FileProvider
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftUiState
import com.example.lagallens.presentation.feature.chatbot.draft.save.pdf.DraftPdfDocumentWriter
import com.example.lagallens.presentation.feature.chatbot.draft.ui.AppliedDraftPreviewActivity
import com.example.lagallens.presentation.feature.chatbot.draft.ui.ChatbotDraftPreviewActivity
import com.example.lagallens.presentation.feature.documentviewer.ui.DocumentViewerActivity
import com.example.lagallens.domain.model.DraftStage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Before
import org.junit.After
import java.io.File
import java.util.UUID
import com.example.lagallens.data.repository.FakeDraftRepository

class DraftVersionDeviceTest {
    @get:org.junit.Rule val demoSession = com.example.lagallens.DemoSessionRule()
    private lateinit var fixtureId: String
    private lateinit var repositoryName: String
    private lateinit var legacyName: String
    private lateinit var app: LegalLensApplication

    @Before fun setUpFixture() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName == "com.example.lagallens.testsandbox")
        app = context.applicationContext as LegalLensApplication
        fixtureId = UUID.randomUUID().toString()
        repositoryName = "draft_fixture_$fixtureId"
        legacyName = "draft_legacy_fixture_$fixtureId"
        val repository = FakeDraftRepository(context, repositoryName, legacyName)
        app.setSandboxDraftFixture(repository, "$fixtureId:${UUID.randomUUID()}", legacyName, fixtureId)
    }

    @After fun removeFixture() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        app.setSandboxDraftFixture(null, null, null, null)
        context.deleteSharedPreferences(repositoryName)
        context.deleteSharedPreferences(legacyName)
        val directory = File(context.cacheDir, "draft-pdfs/$fixtureId")
        if (directory.exists()) {
            directory.listFiles().orEmpty().forEach { file ->
                check(file.parentFile?.canonicalFile == directory.canonicalFile && file.isFile)
                check(file.delete())
            }
            check(directory.delete())
        }
    }
    @Test fun d1AndD3KeepTheirVersionAfterRecreationAndBackground() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = (context.applicationContext as LegalLensApplication).draftRepository
        val expectedTitles = runBlocking {
            val source = repository.getLatestDraft(DraftStage.ORIGINAL) ?: repository.saveDraft(
                DraftStage.ORIGINAL,
                context.getString(R.string.chatbot_draft_document_name, ChatbotDraftUiState.DRAFT_VERSION),
                "HỢP ĐỒNG LAO ĐỘNG\nĐIỀU 1. CÔNG VIỆC\nNội dung DEMO đã lưu."
            )
            val copy = checkNotNull(repository.createCopy(source.draftId, context.getString(R.string.chatbot_draft_document_name, ChatbotDraftUiState.COPY_VERSION)))
            mapOf(ChatbotDraftUiState.DRAFT_VERSION to source.title, ChatbotDraftUiState.COPY_VERSION to copy.title)
        }
        expectedTitles.forEach { (version, title) ->
            ActivityScenario.launch<ChatbotDraftPreviewActivity>(ChatbotDraftPreviewActivity.createIntent(context, version)).use { scenario ->
                fun checkVersion() = onView(withId(R.id.draftContractName)).check(matches(withText(title)))
                checkVersion()
                scenario.recreate()
                checkVersion()
                scenario.moveToState(Lifecycle.State.CREATED)
                scenario.moveToState(Lifecycle.State.RESUMED)
                checkVersion()
            }
        }
    }

    @Test fun d2SaveAndPdfGenerationReachSavedDetail() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = (context.applicationContext as LegalLensApplication).draftRepository
        runBlocking {
            val source = repository.getLatestDraft(DraftStage.ORIGINAL) ?: repository.saveDraft(
                DraftStage.ORIGINAL,
                context.getString(R.string.chatbot_draft_document_name, ChatbotDraftUiState.DRAFT_VERSION),
                "HỢP ĐỒNG LAO ĐỘNG\nĐIỀU 1. CÔNG VIỆC\nNội dung DEMO đã lưu."
            )
            repository.saveDraft(
                DraftStage.AI_PROPOSAL,
                context.getString(R.string.chatbot_draft_document_name, ChatbotDraftUiState.APPROVED_AI_VERSION),
                "HỢP ĐỒNG LAO ĐỘNG\nĐIỀU 1. CÔNG VIỆC\nNội dung D2 DEMO đã lưu.",
                sourceDraftId = source.draftId
            )
        }
        ActivityScenario.launch<AppliedDraftPreviewActivity>(AppliedDraftPreviewActivity.createIntent(context)).use {
            onView(withId(R.id.draftAppliedSave)).perform(click())
            onView(withId(R.id.saveDraftConfirm)).perform(scrollTo(), click())
            SystemClock.sleep(300)
            onView(withId(R.id.saveDraftSuccess)).check(matches(isDisplayed()))
            onView(withId(R.id.saveDraftViewSaved)).perform(scrollTo(), click())
            onView(withId(R.id.savedDraftTitle)).check(matches(withText(context.getString(R.string.chatbot_draft_document_name, ChatbotDraftUiState.APPROVED_AI_VERSION))))
            onView(withId(R.id.savedDraftView)).perform(click())
            onView(withId(R.id.draftAppliedVersionBadge)).check(matches(withText(R.string.chatbot_draft_applied_badge_saved)))
            pressBack()
            onView(withId(R.id.savedDraftExport)).perform(click())
            onView(withId(R.id.pdfCreate)).perform(scrollTo(), click())
            SystemClock.sleep(3000)
            onView(withId(R.id.pdfReady)).check(matches(isDisplayed()))
            onView(withId(R.id.pdfSaveFileReady)).check(matches(isDisplayed()))
        }
    }

    @Test fun generatedD2PdfOpensAsAReadableDocument() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val content = "HỢP ĐỒNG LAO ĐỘNG\nĐIỀU 1. CÔNG VIỆC\nNội dung DEMO đã lưu."
        val file = DraftPdfDocumentWriter.generate(context, "Hợp đồng lao động DEMO — D2", content)
        try {
            var pageCount = 0
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    assertTrue(renderer.pageCount > 0)
                    pageCount = renderer.pageCount
                }
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.draftpdf", file)
            ActivityScenario.launch<DocumentViewerActivity>(DocumentViewerActivity.createIntent(context, uri, "Generated D2 PDF")).use {
                SystemClock.sleep(500)
                onView(withId(R.id.viewerContent)).check(matches(isDisplayed()))
                onView(withId(R.id.viewerPage)).check(matches(isDisplayed()))
                onView(withId(R.id.viewerPageCount)).check(matches(withText(context.getString(R.string.viewer_page_count, 1, pageCount))))
            }
        } finally {
            file.delete()
        }
    }
}
