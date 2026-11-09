package com.example.lagallens

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.example.lagallens.data.datasource.local.datastore.AuthLocalDataSource
import com.example.lagallens.data.repository.DraftContentFactory
import com.example.lagallens.domain.model.DraftStage
import com.example.lagallens.presentation.feature.chatbot.draft.ui.ChatbotDraftAiEditingActivity
import com.example.lagallens.presentation.feature.chatbot.draft.ui.ChatbotDraftVersionsActivity
import com.example.lagallens.presentation.feature.chatbot.ui.ChatbotActivity
import com.example.lagallens.presentation.feature.contracts.detail.ui.ContractDetailFragment
import com.example.lagallens.presentation.feature.main.ui.MainActivity
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Runs in a fresh fixture account of the test sandbox. */
class FreezeFollowUpDeviceTest {
    private val app = ApplicationProvider.getApplicationContext<Context>().applicationContext as LegalLensApplication

    @Before fun signIn() {
        check(app.packageName == "com.example.lagallens.testsandbox")
        check(AuthLocalDataSource(app).saveSession("freeze-followup-${UUID.randomUUID()}@example.invalid"))
    }

    private fun waitUntilDisplayed(viewId: Int) {
        val deadline = SystemClock.uptimeMillis() + 10_000
        while (true) {
            try {
                onView(withId(viewId)).check(matches(isDisplayed()))
                return
            } catch (failure: Throwable) {
                if (SystemClock.uptimeMillis() > deadline) throw failure
                Thread.sleep(200)
            }
        }
    }

    @Test fun openingAMissingContractReturnsWithoutShowingDetail() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val host = activity.supportFragmentManager.findFragmentById(R.id.mainNavHost) as NavHostFragment
                host.navController.navigate(
                    R.id.contractDetailFragment,
                    Bundle().apply { putString(ContractDetailFragment.ARG_CONTRACT_ID, "missing-${UUID.randomUUID()}") }
                )
            }
            waitUntilDisplayed(R.id.navHome)
            onView(withId(R.id.tvContractTitle)).check(doesNotExist())
        }
    }

    @Test fun aiEditProposalIsReviewedAndAppliedToTheSameDraftLine() {
        val contractId = "office-lease"
        val conversationId = "conversation-${UUID.randomUUID()}"
        val source = runBlocking {
            app.draftRepository.saveDraft(
                DraftStage.ORIGINAL,
                "Fixture D1",
                DraftContentFactory.create(app, DraftStage.ORIGINAL),
                contractId = contractId,
                conversationId = conversationId
            )
        }
        val intent = ChatbotDraftAiEditingActivity.createIntent(
            app, "Điều 1", "Làm rõ công việc", false, false, source.draftId, contractId, conversationId
        )
        ActivityScenario.launch<ChatbotDraftAiEditingActivity>(intent).use {
            waitUntilDisplayed(R.id.draftAiReview)
            onView(withId(R.id.draftAiReview)).perform(click())
            waitUntilDisplayed(R.id.draftAiReviewAccept)
            onView(withId(R.id.draftAiReviewAccept)).perform(scrollTo(), click())
            onView(withId(R.id.draftAiReviewContinue)).perform(click())
            onView(withText(R.string.chatbot_draft_ai_approval_confirm)).inRoot(isDialog()).perform(click())
            waitUntilDisplayed(R.id.draftAiApplyView)
        }
        val applied = runBlocking { app.draftRepository.getDrafts() }.filter { it.draftId != source.draftId }
        assertTrue(applied.isNotEmpty())
        applied.forEach { draft ->
            assertEquals(contractId, draft.contractId)
            assertEquals(conversationId, draft.conversationId)
        }
    }

    @Test fun chatRequestCreatesD1AndOpensItsPreview() {
        val intent = ChatbotActivity.createNewConversationIntent(
            app,
            app.getString(R.string.chatbot_default_contract_name),
            app.getString(R.string.chatbot_default_contract_type)
        )
        ActivityScenario.launch<ChatbotActivity>(intent).use {
            waitUntilDisplayed(R.id.chatbotNewSuggestionRisk)
            onView(withId(R.id.chatbotNewSuggestionRisk)).perform(click())
            onView(withText(R.string.chatbot_draft_clarification)).check(matches(isDisplayed()))
            onView(withId(R.id.chatbotInput)).perform(replaceText(app.getString(R.string.chatbot_draft_sample_details)), closeSoftKeyboard())
            onView(withId(R.id.chatbotSend)).perform(click())
            waitUntilDisplayed(R.id.chatbotDraftReviewAction)
            onView(withId(R.id.chatbotDraftReviewAction)).perform(click())
            onView(withId(R.id.chatbotDraftCreate)).perform(click())
            waitUntilDisplayed(R.id.chatbotGenerationView)
            onView(withId(R.id.chatbotGenerationView)).perform(click())
            waitUntilDisplayed(R.id.draftContractName)
        }
        assertTrue(runBlocking { app.draftRepository.getDrafts() }.any { it.stage == DraftStage.ORIGINAL })
    }

    @Test fun copyFromD1CreatesD3ThroughTheVersionsScreen() {
        val contractId = "office-lease"
        val source = runBlocking {
            app.draftRepository.saveDraft(DraftStage.ORIGINAL, "Fixture D1", DraftContentFactory.create(app, DraftStage.ORIGINAL), contractId = contractId)
        }
        ActivityScenario.launch<ChatbotDraftVersionsActivity>(ChatbotDraftVersionsActivity.createIntent(app, contractId = contractId)).use {
            waitUntilDisplayed(R.id.draftVersionsBack)
            onView(withId(R.id.draftVersionD1Copy)).perform(scrollTo(), click())
            onView(withText(R.string.chatbot_draft_copy_confirm_action)).inRoot(isDialog()).perform(click())
            waitUntilDisplayed(R.id.copyDraftView)
            onView(withId(R.id.copyDraftView)).perform(click())
            waitUntilDisplayed(R.id.draftContractName)
        }
        val copies = runBlocking { app.draftRepository.getDrafts() }.filter { it.stage == DraftStage.COPY }
        assertTrue(copies.any { it.contractId == contractId && it.content == source.content })
    }
}
