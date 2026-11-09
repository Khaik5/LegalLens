package com.example.lagallens

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.lifecycle.Lifecycle
import com.example.lagallens.data.datasource.local.datastore.AuthLocalDataSource
import com.example.lagallens.domain.model.*
import com.example.lagallens.domain.repository.*
import com.example.lagallens.presentation.feature.analysis.processing.ui.AnalysisProcessingActivity
import com.example.lagallens.presentation.feature.analysis.result.ui.AnalysisResultActivity
import com.example.lagallens.presentation.feature.analysis.summary.ui.AnalysisSummaryActivity
import com.example.lagallens.presentation.feature.analysis.keyfacts.ui.AnalysisKeyFactsActivity
import com.example.lagallens.presentation.feature.analysis.parties.ui.AnalysisPartiesActivity
import com.example.lagallens.presentation.feature.analysis.obligations.ui.AnalysisObligationsActivity
import com.example.lagallens.presentation.feature.analysis.risk.ui.AnalysisRiskOverviewActivity
import com.example.lagallens.presentation.feature.analysis.risk.findings.ui.AnalysisRiskFindingsActivity
import com.example.lagallens.presentation.feature.analysis.risk.detail.ui.AnalysisRiskDetailActivity
import com.example.lagallens.presentation.feature.analysis.legalreferences.ui.AnalysisLegalReferencesActivity
import com.example.lagallens.presentation.feature.notifications.viewmodel.NotificationViewModel
import com.example.lagallens.presentation.feature.notifications.contract.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

/** Only random fixture accounts in the test sandbox; no app-data clearing or user-file deletion. */
class FreezeAcceptanceDeviceTest {
    private val app = ApplicationProvider.getApplicationContext<Context>().applicationContext as LegalLensApplication
    private fun session(): String {
        check(app.packageName == "com.example.lagallens.testsandbox")
        check(AuthLocalDataSource(app).saveSession("freeze-${UUID.randomUUID()}@example.invalid"))
        return checkNotNull(app.currentSessionKey())
    }
    private suspend fun result(session: String) = app.analysisIdentityRepository.create(UUID.randomUUID().toString(), "collaborator", "document-collaborator", session)
    private fun scope(intent: Intent, result: String, session: String) = intent
        .putExtra(AnalysisProcessingActivity.EXTRA_RESULT_ID, result)
        .putExtra(AnalysisProcessingActivity.EXTRA_SESSION_KEY, session)

    @Test fun entitlementIsIdempotentAndScopedAndInsufficientBalanceFailsClosed() = runBlocking {
        val owner = session()
        val first = result(owner)
        val other = result(owner)
        val third = result(owner)
        val unlock = app.analysisUnlockRepository
        assertFalse(unlock.status(owner, first.resultId).isUnlocked)
        assertTrue(unlock.unlock(owner, first.resultId) is AnalysisUnlockOutcome.Unlocked)
        assertEquals(3, app.creditRepository.balance.value)
        assertTrue(unlock.unlock(owner, first.resultId) is AnalysisUnlockOutcome.Unlocked)
        assertEquals(3, app.creditRepository.balance.value)
        assertTrue(unlock.status(owner, first.resultId).isUnlocked)
        assertFalse(unlock.status("wrong-session", first.resultId).isUnlocked)
        assertFalse(unlock.status(owner, other.resultId).isUnlocked)
        assertTrue(unlock.unlock(owner, other.resultId) is AnalysisUnlockOutcome.Unlocked)
        assertTrue(unlock.unlock(owner, third.resultId) is AnalysisUnlockOutcome.Insufficient)
        assertEquals(1, app.creditRepository.balance.value)
        assertFalse(unlock.status(owner, third.resultId).isUnlocked)
        val otherAccount = session()
        assertFalse(unlock.status(otherAccount, first.resultId).isUnlocked)
        assertEquals(AnalysisUnlockOutcome.Unavailable, unlock.unlock(owner, first.resultId))
    }

    @Test fun lockedResultCannotOpenLockedRiskDirectly() = runBlocking<Unit> {
        val owner = session(); val result = result(owner)
        val screens = listOf(AnalysisRiskDetailActivity::class.java)
        screens.forEach { screen ->
            ActivityScenario.launch<android.app.Activity>(scope(AnalysisRiskDetailActivity.createIntent(app, "termination-notice"), result.resultId, owner)).use { scenario ->
                val deadline = android.os.SystemClock.uptimeMillis() + 5000
                while (scenario.state != Lifecycle.State.DESTROYED && android.os.SystemClock.uptimeMillis() < deadline) android.os.SystemClock.sleep(50)
                assertEquals(screen.simpleName, Lifecycle.State.DESTROYED, scenario.state)
            }
        }
    }

    @Test fun severityRendersFromSnapshotAndRiskFilterSurvivesBack() = runBlocking<Unit> {
        val owner = session(); val result = result(owner)
        app.analysisUnlockRepository.unlock(owner, result.resultId)
        ActivityScenario.launch<AnalysisResultActivity>(scope(AnalysisResultActivity.createIntent(app, "Fixture", "DEMO"), result.resultId, owner)).use {
            onView(withId(R.id.analysis_result_severity)).check(matches(withText(R.string.freeze_severity_high)))
        }
        ActivityScenario.launch<AnalysisRiskFindingsActivity>(scope(Intent(app, AnalysisRiskFindingsActivity::class.java), result.resultId, owner)).use {
            onView(withId(R.id.analysisRiskFindingsFilterLow)).perform(click())
            val title = result.snapshot!!["${app.resources.configuration.locales[0].language}:analysis_risk_findings_dispute_title"]
            // Click the displayed row, then return; the selected filter must still have one row.
            onView(withId(R.id.analysisRiskFindingsItems)).check { view, _ ->
                assertEquals(1, (view as androidx.recyclerview.widget.RecyclerView).adapter?.itemCount)
                view.findViewHolderForAdapterPosition(0)?.itemView?.performClick()
            }
            pressBack()
            onView(withId(R.id.analysisRiskFindingsItems)).check { view, _ ->
                assertEquals(1, (view as androidx.recyclerview.widget.RecyclerView).adapter?.itemCount)
            }
        }
    }

    @Test fun notificationWriteFailuresAreVisibleWithoutFalseSuccessOrReadState() = runBlocking<Unit> {
        session()
        val records = listOf(NotificationRecord("n", null, NotificationKind.ANALYSIS, true, "Fixture", "Fixture", 1))
        val repository = object : NotificationRepository {
            override suspend fun getAll() = records
            override suspend fun markRead(notificationId: String): Unit = throw java.io.IOException("fixture")
            override suspend fun markAllRead(): Unit = throw java.io.IOException("fixture")
        }
        val vm = withContext(Dispatchers.Main) { NotificationViewModel(app, repository).also { it.onEvent(NotificationUiEvent.ScreenOpened) } }
        withTimeout(5000) { vm.uiState.first { it.notifications.isNotEmpty() } }
        withContext(Dispatchers.Main) { vm.onEvent(NotificationUiEvent.NotificationClicked("n")) }
        assertEquals(NotificationUiEffect.ShowMessage(R.string.operation_network_error), withTimeout(5000) { vm.uiEffect.first() })
        assertTrue(vm.uiState.value.notifications.single().isUnread)
        withContext(Dispatchers.Main) { vm.onEvent(NotificationUiEvent.MarkAllReadClicked) }
        assertEquals(NotificationUiEffect.ShowMessage(R.string.operation_network_error), withTimeout(5000) { vm.uiEffect.first() })
        assertTrue(vm.uiState.value.notifications.single().isUnread)
    }

    @Test fun highRiskIncludesCritical() = runBlocking<Unit> {
        session()
        app.contractRepository.createContract(EmploymentContract("critical", "critical-doc", "Critical fixture", "DEMO", 1, 1, risk = EmploymentContractRisk.CRITICAL))
        val vm = withContext(Dispatchers.Main) { com.example.lagallens.presentation.feature.contracts.viewmodel.ContractListViewModel(app) }
        withTimeout(5000) { vm.uiState.first { it.contracts.any { item -> item.id == "critical" } } }
        withContext(Dispatchers.Main) { vm.onEvent(com.example.lagallens.presentation.feature.contracts.contract.ContractListUiEvent.FilterSelected(com.example.lagallens.presentation.feature.contracts.contract.ContractFilter.HIGH_RISK)) }
        assertTrue(vm.uiState.value.contracts.any { it.id == "critical" })
    }
}
