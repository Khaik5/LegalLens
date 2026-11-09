package com.example.lagallens

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lagallens.data.datasource.local.datastore.AuthLocalDataSource
import com.example.lagallens.data.repository.SessionScopedEmploymentRepositories
import com.example.lagallens.presentation.feature.analysis.processing.ui.AnalysisProcessingActivity
import com.example.lagallens.presentation.feature.analysis.summary.ui.AnalysisSummaryActivity
import com.example.lagallens.presentation.feature.analysis.keyfacts.ui.AnalysisKeyFactsActivity
import com.example.lagallens.presentation.feature.analysis.parties.ui.AnalysisPartiesActivity
import com.example.lagallens.presentation.feature.analysis.obligations.ui.AnalysisObligationsActivity
import com.example.lagallens.presentation.feature.analysis.risk.detail.ui.AnalysisRiskDetailActivity
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Creates a unique sandbox account and does not clear any application data. */
@RunWith(AndroidJUnit4::class)
class AnalysisSnapshotScreensDeviceTest {
    @Test fun fiveScreensRenderAndSurviveActivityRecreation() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        check(context.packageName == "com.example.lagallens.testsandbox")
        val unique = UUID.randomUUID().toString()
        val auth = AuthLocalDataSource(context)
        check(auth.saveSession("screens-$unique@example.invalid"))
        val repository = SessionScopedEmploymentRepositories(context, auth).analysisIdentities
        val session = checkNotNull(repository.sessionKey())
        val result = repository.create("screens-$unique", "collaborator", "document-collaborator", session)
        (context.applicationContext as com.example.lagallens.LegalLensApplication).analysisUnlockRepository.unlock(session, result.resultId)
        fun text(key: String): org.hamcrest.Matcher<String> = org.hamcrest.Matchers.anyOf(
            org.hamcrest.Matchers.equalTo(checkNotNull(result.snapshot?.get("vi:$key"))),
            org.hamcrest.Matchers.equalTo(checkNotNull(result.snapshot?.get("en:$key")))
        )
        fun scoped(intent: Intent) = intent.putExtra(AnalysisProcessingActivity.EXTRA_RESULT_ID, result.resultId)
            .putExtra(AnalysisProcessingActivity.EXTRA_SESSION_KEY, session)
        ActivityScenario.launch<AnalysisSummaryActivity>(scoped(AnalysisSummaryActivity.createIntent(context, "demo", "demo"))).use {
            onView(withText(text("analysis_summary_contract_type_title"))).check(matches(isDisplayed()))
            onView(withId(R.id.analysis_summary_sections)).check(matches(isDisplayed())); it.recreate()
            onView(withId(R.id.analysis_summary_sections)).check(matches(isDisplayed()))
        }
        ActivityScenario.launch<AnalysisKeyFactsActivity>(scoped(AnalysisKeyFactsActivity.createIntent(context, "demo", "demo"))).use {
            onView(withText(text("analysis_key_facts_salary_label"))).check(matches(isDisplayed()))
            onView(withId(R.id.analysis_key_facts_items)).check(matches(isDisplayed())); it.recreate()
            onView(withId(R.id.analysis_key_facts_items)).check(matches(isDisplayed()))
        }
        ActivityScenario.launch<AnalysisPartiesActivity>(scoped(AnalysisPartiesActivity.createIntent(context, "demo", "demo"))).use {
            onView(withText(text("analysis_parties_employer_title"))).check(matches(isDisplayed()))
            onView(withText(text("analysis_parties_employer_title"))).check(matches(isDisplayed())); it.recreate()
            onView(withText(text("analysis_parties_employer_title"))).check(matches(isDisplayed()))
        }
        ActivityScenario.launch<AnalysisObligationsActivity>(scoped(AnalysisObligationsActivity.createIntent(context, "demo", "demo"))).use {
            onView(withText(text("analysis_obligations_employer_heading"))).check(matches(isDisplayed()))
            onView(withId(R.id.analysis_obligations_items)).check(matches(isDisplayed())); it.recreate()
            onView(withId(R.id.analysis_obligations_items)).check(matches(isDisplayed()))
        }
        ActivityScenario.launch<AnalysisRiskDetailActivity>(scoped(AnalysisRiskDetailActivity.createIntent(context, "penalty-cap"))).use {
            onView(withId(R.id.analysisRiskDetailTitle)).check(matches(withText(org.hamcrest.Matchers.anyOf(
                org.hamcrest.Matchers.containsString(checkNotNull(result.snapshot?.get("vi:analysis_risk_findings_penalty_title"))),
                org.hamcrest.Matchers.containsString(checkNotNull(result.snapshot?.get("en:analysis_risk_findings_penalty_title")))
            ))))
            onView(withId(R.id.analysisRiskDetailTitle)).check(matches(isDisplayed())); it.recreate()
            onView(withId(R.id.analysisRiskDetailTitle)).check(matches(isDisplayed()))
        }
    }
}
