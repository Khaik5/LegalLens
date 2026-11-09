package com.example.lagallens

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lagallens.domain.model.ProfileLanguage
import com.example.lagallens.presentation.feature.chatbot.ui.ChatbotActivity
import com.example.lagallens.presentation.feature.compare.select.ui.CompareSelectActivity
import com.example.lagallens.presentation.feature.creditstore.ui.CreditStoreActivity
import com.example.lagallens.presentation.feature.home.ui.HomeActivity
import com.example.lagallens.presentation.feature.main.ui.MainActivity
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Walks the demo flow by tapping the real UI and saves one screenshot per step for the Figma review.
 * It is skipped unless the runner argument `screenTour` is set, e.g. `-e screenTour vi-light`.
 * Variants: `vi-light`, `en-light`; the language comes from the prefix. The app is locked to the light theme, so a run
 * with the device in dark mode (`adb shell cmd uimode night yes`) should look the same. Screenshots land in the app's external files dir under `tour/<variant>`.
 */
class ScreenTourDeviceTest {
    private val app = ApplicationProvider.getApplicationContext<Context>().applicationContext as LegalLensApplication
    private val variant: String? = InstrumentationRegistry.getArguments().getString("screenTour")
    private lateinit var directory: File
    private var index = 0
    private val failures = mutableListOf<String>()

    @Test fun walkTheDemoFlow() {
        val tour = variant ?: return
        check(app.packageName == "com.example.lagallens.testsandbox")
        directory = File(app.getExternalFilesDir(null), "tour/$tour").apply { deleteRecursively(); mkdirs() }
        runBlocking {
            app.updateProfileLanguageUseCase(if (tour.startsWith("en")) ProfileLanguage.ENGLISH else ProfileLanguage.VIETNAMESE)
            runCatching { app.logoutUseCase() }
        }
        Thread.sleep(1_500)
        try {
            part("auth-analysis", ::authAndAnalysis)
            part("chatbot-drafts", ::chatbotAndDrafts)
            part("credits", ::credits)
            part("tabs-compare-upload", ::tabsCompareAndUpload)
        } finally {
            runBlocking {
                app.updateProfileLanguageUseCase(ProfileLanguage.VIETNAMESE)
            }
        }
        File(directory, "failures.txt").writeText(failures.joinToString("\n"))
        assertTrue("Tour steps failed: $failures", failures.isEmpty())
    }

    private fun part(name: String, block: () -> Unit) {
        try {
            block()
        } catch (failure: Throwable) {
            // The screenshot is taken after the failing screen has closed, so it shows the next state, not the failure.
            failures += "$name after step $index: ${failure.javaClass.simpleName}: ${failure.message?.lineSequence()?.firstOrNull()}"
        }
    }

    private fun authAndAnalysis() {
        ActivityScenario.launch(HomeActivity::class.java).use {
            waitFor(R.id.loginButton); shot("home")
            onView(withId(R.id.loginButton)).perform(click())
            waitFor(R.id.submitButton); shot("login")
            onView(withId(R.id.emailInput)).perform(replaceText("tour-${UUID.randomUUID().toString().take(8)}@example.com"))
            onView(withId(R.id.passwordInput)).perform(replaceText("demo123"), closeSoftKeyboard())
            onView(withId(R.id.submitButton)).perform(click())
            waitFor(R.id.navHome); Thread.sleep(800); shot("dashboard")
            onView(withId(R.id.navContracts)).perform(click())
            Thread.sleep(800); shot("contracts")
            onView(seededContractTitle(R.string.contract_demo_labor_indefinite)).perform(click())
            waitFor(R.id.tvContractTitle); shot("contract-detail")
            onView(withText(R.string.contract_detail_analyze)).perform(click())
            waitFor(R.id.analysis_start_action); shot("analysis-start")
            onView(withId(R.id.analysis_start_action)).perform(click())
            Thread.sleep(1_200); shot("analysis-processing")
            waitFor(R.id.analysis_result_items, 30_000); Thread.sleep(500); shot("analysis-result")
            // Every section opens before unlocking and shows its free preview with the locked card.
            listOf(
                Triple(R.string.analysis_result_important_information_title, R.id.analysis_key_facts_root, "preview-key-facts"),
                Triple(R.string.analysis_result_parties_title, R.id.analysis_parties_root, "preview-parties"),
                Triple(R.string.analysis_result_rights_title, R.id.analysis_obligations_root, "preview-obligations-rights"),
                Triple(R.string.analysis_result_legal_basis_title, R.id.analysisLegalReferencesRoot, "preview-legal-basis")
            ).forEach { (title, root, name) ->
                onView(allOf(withText(title), isDescendantOfA(withId(R.id.analysis_result_items)))).perform(scrollTo(), click())
                waitFor(root); Thread.sleep(600); shot(name)
                if (root == R.id.analysis_obligations_root) {
                    onView(withId(R.id.analysis_obligations_tab_duties)).perform(click())
                    Thread.sleep(500); shot("preview-obligations-duties")
                }
                pressBack()
                waitFor(R.id.analysis_result_items)
            }
            onView(allOf(withText(R.string.analysis_result_risk_title), isDescendantOfA(withId(R.id.analysis_result_items)))).perform(scrollTo(), click())
            waitFor(R.id.analysisRiskOverviewViewDetails)
            onView(withId(R.id.analysisRiskOverviewViewDetails)).perform(scrollTo(), click())
            Thread.sleep(1_000); shot("preview-risk-findings")
            pressBack(); Thread.sleep(500); pressBack()
            waitFor(R.id.analysis_result_items)
            onView(withText(R.string.analysis_result_summary_title)).perform(scrollTo(), click())
            waitFor(R.id.analysisLockedUnlock); Thread.sleep(800); shot("analysis-summary-preview")
            onView(withId(R.id.analysisLockedUnlock)).perform(scrollTo(), click())
            waitFor(R.id.unlockConfirmationConfirm); shot("unlock-confirmation")
            onView(withId(R.id.unlockConfirmationConfirm)).perform(click())
            Thread.sleep(1_500); shot("unlock-success")
            // The success dialog animates continuously, so Espresso never sees an idle root; tap its button directly.
            clickInDialog(R.id.unlockSuccessViewResult)
            Thread.sleep(1_500)
            val summaryOpen = runCatching { onView(withId(R.id.analysis_summary_root)).check(matches(isDisplayed())) }.isSuccess
            if (!summaryOpen) {
                waitFor(R.id.analysis_result_items)
                onView(withText(R.string.analysis_result_summary_title)).perform(scrollTo(), click())
            }
            waitFor(R.id.analysis_summary_root); Thread.sleep(500); shot("analysis-summary")
            pressBack()
            waitFor(R.id.analysis_result_items)
            onView(allOf(withText(R.string.analysis_result_risk_title), isDescendantOfA(withId(R.id.analysis_result_items)))).perform(scrollTo(), click())
            waitFor(R.id.analysisRiskOverviewRoot); Thread.sleep(500); shot("analysis-risk")
        }
    }

    private fun chatbotAndDrafts() {
        val intent = ChatbotActivity.createIntent(
            app, app.getString(R.string.chatbot_default_contract_name), app.getString(R.string.chatbot_default_contract_type), "office-lease"
        )
        ActivityScenario.launch<ChatbotActivity>(intent).use {
            waitFor(R.id.chatbotSend); Thread.sleep(800); shot("chatbot")
            onView(withId(R.id.chatbotMore)).perform(click())
            waitFor(R.id.chatbotNewSuggestionRisk); shot("chatbot-new")
            onView(withId(R.id.chatbotNewSuggestionRisk)).perform(click())
            Thread.sleep(2_500); shot("chatbot-draft-question")
            onView(withId(R.id.chatbotInput)).perform(replaceText(app.getString(R.string.chatbot_draft_sample_details)), closeSoftKeyboard())
            onView(withId(R.id.chatbotSend)).perform(click())
            waitFor(R.id.chatbotDraftReviewAction); shot("chatbot-draft-answer")
            onView(withId(R.id.chatbotDraftReviewAction)).perform(click())
            waitFor(R.id.chatbotDraftCreate); shot("chatbot-draft-review")
            onView(withId(R.id.chatbotDraftCreate)).perform(click())
            Thread.sleep(800); shot("chatbot-draft-generating")
            waitFor(R.id.chatbotGenerationView); shot("chatbot-draft-ready")
            onView(withId(R.id.chatbotGenerationView)).perform(click())
            waitFor(R.id.draftContractName); Thread.sleep(800); shot("draft-d1")
            onView(withId(R.id.draftAskAi)).perform(click())
            waitFor(R.id.draftAiSubmit); shot("draft-ai-request")
            onView(withId(R.id.draftAiSubmit)).perform(click())
            waitFor(R.id.draftAiReview); shot("draft-ai-proposal")
            onView(withId(R.id.draftAiReview)).perform(click())
            waitFor(R.id.draftAiReviewAccept); shot("draft-ai-review")
            onView(withId(R.id.draftAiReviewAccept)).perform(scrollTo(), click())
            onView(withId(R.id.draftAiReviewContinue)).perform(click())
            onView(withText(R.string.chatbot_draft_ai_approval_confirm)).inRoot(isDialog()).perform(click())
            waitFor(R.id.draftAiApplyView); shot("draft-ai-applied")
            onView(withId(R.id.draftAiApplyView)).perform(click())
            waitFor(R.id.draftAppliedManage); Thread.sleep(500); shot("draft-d2")
            onView(withId(R.id.draftAppliedManage)).perform(click())
            waitFor(R.id.draftVersionsBack); Thread.sleep(500); shot("draft-versions")
            onView(withId(R.id.draftVersionD1Copy)).perform(scrollTo(), click())
            onView(withText(R.string.chatbot_draft_copy_confirm_action)).inRoot(isDialog()).perform(click())
            waitFor(R.id.copyDraftView); shot("draft-d3-created")
            onView(withId(R.id.copyDraftView)).perform(click())
            waitFor(R.id.draftContractName); Thread.sleep(500); shot("draft-d3")
        }
    }

    private fun credits() {
        ActivityScenario.launch(CreditStoreActivity::class.java).use {
            waitFor(R.id.creditStoreBack); Thread.sleep(800); shot("credit-store")
            onView(allOf(withId(R.id.creditStorePackageTitle), withText(R.string.credit_store_package_5_title))).perform(scrollTo(), click())
            onView(withId(R.id.creditStoreTopUpAction)).perform(scrollTo(), click())
            waitFor(R.id.purchaseConfirmationPay); shot("credit-confirmation")
            onView(withId(R.id.purchaseConfirmationPay)).perform(click())
            Thread.sleep(500); shot("credit-pending")
            waitFor(R.id.purchaseSuccessReturnToUnlock); shot("credit-success")
        }
        ActivityScenario.launch(CreditStoreActivity::class.java).use {
            waitFor(R.id.creditStoreBack)
            onView(withId(R.id.creditStoreViewAllHistory)).perform(scrollTo(), click())
            Thread.sleep(1_500); shot("credit-history")
        }
    }

    private fun tabsCompareAndUpload() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitFor(R.id.navNotifications)
            onView(withId(R.id.navNotifications)).perform(click())
            Thread.sleep(1_000); shot("notifications")
            onView(withId(R.id.navProfile)).perform(click())
            Thread.sleep(1_000); shot("profile")
            onView(withId(R.id.btnAddContract)).perform(click())
            Thread.sleep(2_000); shot("upload-source")
            pressBack()
        }
        ActivityScenario.launch<CompareSelectActivity>(CompareSelectActivity.createIntent(app)).use {
            waitFor(R.id.compareSelectSubmit); Thread.sleep(500)
            onView(seededContractTitle(R.string.contract_demo_labor_indefinite)).perform(click())
            onView(seededContractTitle(R.string.contract_demo_labor_fixed_term)).perform(click())
            shot("compare-select")
            onView(withId(R.id.compareSelectSubmit)).perform(click())
            waitFor(R.id.compareResultRoot); Thread.sleep(1_000); shot("compare-result")
        }
    }

    private fun clickInDialog(viewId: Int) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        var clicked = false
        instrumentation.runOnMainSync {
            ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                .filterIsInstance<FragmentActivity>()
                .flatMap { it.supportFragmentManager.fragments }
                .filterIsInstance<DialogFragment>()
                .firstNotNullOfOrNull { it.dialog?.findViewById<View>(viewId) ?: it.view?.findViewById(viewId) }
                ?.let { clicked = it.performClick() }
        }
        check(clicked) { "Dialog button not found" }
    }

    private fun waitFor(viewId: Int, timeoutMillis: Long = 15_000) {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        while (true) {
            try {
                onView(withId(viewId)).check(matches(isDisplayed()))
                return
            } catch (failure: Throwable) {
                if (SystemClock.uptimeMillis() > deadline) throw failure
                Thread.sleep(250)
            }
        }
    }

    private fun shot(name: String) {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: return
        index++
        val file = File(directory, "%02d-%s.png".format(index, name))
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        Log.i("SCREEN_TOUR", "saved ${file.name}")
    }
}
