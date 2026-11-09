package com.example.lagallens

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lagallens.presentation.feature.analysis.obligations.ui.AnalysisObligationsActivity
import com.example.lagallens.presentation.feature.analysis.parties.ui.AnalysisPartiesActivity
import com.example.lagallens.presentation.feature.analysis.processing.ui.AnalysisProcessingActivity
import com.example.lagallens.presentation.feature.analysis.risk.findings.ui.AnalysisRiskFindingsActivity
import com.example.lagallens.presentation.feature.analysis.summary.ui.AnalysisSummaryActivity
import com.example.lagallens.presentation.feature.analysis.keyfacts.ui.AnalysisKeyFactsActivity
import com.example.lagallens.presentation.feature.analysis.legalreferences.ui.AnalysisLegalReferencesActivity
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AnalysisPreviewFlowDeviceTest {
    private val app = ApplicationProvider.getApplicationContext<Context>().applicationContext as LegalLensApplication

    private fun waitFor(id: Int) {
        val deadline = SystemClock.uptimeMillis() + 20_000
        var failure: Throwable? = null
        while (SystemClock.uptimeMillis() < deadline) {
            try { onView(withId(id)).check(matches(isDisplayed())); return } catch (t: Throwable) { failure = t; Thread.sleep(150) }
        }
        throw AssertionError("View $id not visible", failure)
    }

    @Test fun allSixSectionsShowPreviewAndUnlockTogether() {
        val scoped = AnalysisTestFixture.scoped(AnalysisSummaryActivity.createIntent(app, "demo", "PDF"), unlocked = false)
        val result = scoped.getStringExtra(AnalysisProcessingActivity.EXTRA_RESULT_ID)!!
        val session = scoped.getStringExtra(AnalysisProcessingActivity.EXTRA_SESSION_KEY)!!
        fun scopedTo(intent: Intent) = intent.putExtra(AnalysisProcessingActivity.EXTRA_RESULT_ID, result).putExtra(AnalysisProcessingActivity.EXTRA_SESSION_KEY, session)
        val screens = listOf(
            scopedTo(AnalysisSummaryActivity.createIntent(app, "demo", "PDF")),
            scopedTo(AnalysisKeyFactsActivity.createIntent(app, "demo", "PDF")),
            scopedTo(AnalysisPartiesActivity.createIntent(app, "demo", "PDF")),
            scopedTo(AnalysisObligationsActivity.createIntent(app, "demo", "PDF")),
            scopedTo(AnalysisRiskFindingsActivity.createIntent(app, "demo", "PDF")),
            scopedTo(AnalysisLegalReferencesActivity.createIntent(app, "demo", "PDF"))
        )
        screens.forEachIndexed { index, intent ->
            ActivityScenario.launch<android.app.Activity>(intent).use {
                val button = if (index == 5) R.id.analysisLegalReferencesUnlock else R.id.analysisLockedUnlock
                waitFor(button)
                if (index == 0) {
                    // The free part must be visible, not only the locked card.
                    Thread.sleep(800)
                    onView(withText(R.string.analysis_summary_contract_type_title)).check(matches(isDisplayed()))
                }
            }
        }
        runBlocking { assertTrue(app.analysisUnlockRepository.unlock(session, result) is com.example.lagallens.domain.repository.AnalysisUnlockOutcome.Unlocked) }
        ActivityScenario.launch<AnalysisRiskFindingsActivity>(screens[4]).use {
            waitFor(R.id.analysisRiskFindingsItems)
            Thread.sleep(800)
            onView(withId(R.id.analysisLockedUnlock)).check(androidx.test.espresso.assertion.ViewAssertions.doesNotExist())
        }
        ActivityScenario.launch<AnalysisLegalReferencesActivity>(screens[5]).use {
            waitFor(R.id.analysisLegalReferencesList)
            Thread.sleep(800)
            onView(withId(R.id.analysisLegalReferencesLocked)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        }
    }

    @Test fun obligationTabsAndSampleExcerptWork() {
        val intent = AnalysisTestFixture.scoped(AnalysisObligationsActivity.createIntent(app, "demo", "PDF"), unlocked = false)
        ActivityScenario.launch<AnalysisObligationsActivity>(intent).use {
            waitFor(R.id.analysis_obligations_tab_duties)
            onView(withId(R.id.analysis_obligations_tab_duties)).perform(click())
            waitFor(R.id.analysis_obligation_source)
            onView(withId(R.id.analysis_obligation_source)).perform(click())
            waitFor(R.id.analysis_clause_source_excerpt)
            onView(withId(R.id.analysis_clause_source_close_button)).perform(click())
            onView(withId(R.id.analysis_obligations_back)).perform(click())
        }
    }

    @Test fun purchaseReturnsFromCreditHubToRiskWithoutUnlocking() {
        val intent = AnalysisTestFixture.scoped(AnalysisRiskFindingsActivity.createIntent(app, "demo", "PDF"), unlocked = false)
        val result = intent.getStringExtra(AnalysisProcessingActivity.EXTRA_RESULT_ID)!!
        val session = intent.getStringExtra(AnalysisProcessingActivity.EXTRA_SESSION_KEY)!!
        runBlocking { assertTrue(app.creditRepository.spend("preview-${UUID.randomUUID()}", 4).applied) }
        ActivityScenario.launch<AnalysisRiskFindingsActivity>(intent).use {
            waitFor(R.id.analysisLockedUnlock)
            onView(withId(R.id.analysisLockedUnlock)).perform(click())
            waitFor(R.id.unlockConfirmationConfirm)
            onView(withId(R.id.unlockConfirmationConfirm)).perform(click())
            waitFor(R.id.insufficientCreditsStore)
            onView(withId(R.id.insufficientCreditsStore)).perform(click())
            waitFor(R.id.creditStoreBack)
            onView(allOf(withId(R.id.creditStorePackageTitle), withText(R.string.credit_store_package_5_title))).perform(scrollTo(), click())
            onView(withId(R.id.creditStoreTopUpAction)).perform(scrollTo(), click())
            waitFor(R.id.purchaseConfirmationPay)
            onView(withId(R.id.purchaseConfirmationPay)).perform(click())
            waitFor(R.id.purchaseSuccessReturnToUnlock)
            onView(withId(R.id.purchaseSuccessReturnToUnlock)).perform(click())
            waitFor(R.id.creditHubBack)
            onView(withId(R.id.creditHubBack)).perform(click())
            waitFor(R.id.analysisRiskFindingsItems)
            waitFor(R.id.analysisLockedUnlock)
            runBlocking { assertFalse(app.analysisUnlockRepository.status(session, result).isUnlocked) }
            assertTrue(app.creditRepository.balance.value > 1)
        }
    }
}
