package com.example.lagallens.presentation.feature.analysis

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.R
import com.example.lagallens.presentation.feature.analysis.processing.ui.AnalysisProcessingActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Keeps an analysis screen hidden until its result and owner session are revalidated. */
abstract class AnalysisScopedActivity : AppCompatActivity() {
    private var validated = false
    private var validationJob: Job? = null
    /** Paid content stays closed even if a screen is reached without going through the result screen. */
    protected open val requiresUnlock = true
    protected open val observesUnlock = false
    protected var validatedUnlockStatus: com.example.lagallens.domain.repository.AnalysisUnlockStatus? = null
        private set
    private val resultId get() = intent.getStringExtra(AnalysisProcessingActivity.EXTRA_RESULT_ID).orEmpty()
    private val session get() = intent.getStringExtra(AnalysisProcessingActivity.EXTRA_SESSION_KEY).orEmpty()
    protected fun hasValidatedAnalysis(): Boolean = validated &&
        (application as LegalLensApplication).analysisIdentityRepository.sessionKey() == session
    protected fun openUnlockConfirmation(documentName: String?) {
        val status = validatedUnlockStatus ?: return
        if (!hasValidatedAnalysis() || resultId.isBlank() || status.cost <= 0) return
        val app = application as LegalLensApplication
        startActivity(com.example.lagallens.presentation.feature.creditstore.unlock.ui.UnlockConfirmationActivity.createIntent(
            this, resultId, app.creditRepository.balance.value, status.cost, documentName, fromAnalysis = true
        ))
    }
    protected fun showContentStatus(status: SnapshotContentStatus) {
        when (status) {
            SnapshotContentStatus.EMPTY -> Unit
            SnapshotContentStatus.UNAVAILABLE -> { Toast.makeText(this, R.string.analysis_result_unavailable, Toast.LENGTH_LONG).show(); finish() }
            SnapshotContentStatus.ERROR -> { Toast.makeText(this, R.string.analysis_storage_error, Toast.LENGTH_LONG).show(); finish() }
            SnapshotContentStatus.LOADING, SnapshotContentStatus.READY -> Unit
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.alpha = 0f
        lifecycleScope.launch {
            (application as LegalLensApplication).observeSessionKeys().collect { current ->
                if (session.isBlank() || current != session) {
                    validated = false
                    window.decorView.alpha = 0f
                    onAnalysisInvalidated()
                    supportFragmentManager.fragments.filterIsInstance<androidx.fragment.app.DialogFragment>()
                        .forEach { it.dismissAllowingStateLoss() }
                    finish()
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        validated = false
        window.decorView.alpha = 0f
        validationJob = lifecycleScope.launch {
            val app = application as LegalLensApplication
            val repository = app.analysisIdentityRepository
            val result = try { if (repository.sessionKey() == session && session.isNotBlank()) repository.get(resultId, session) else null }
                catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (_: com.example.lagallens.domain.repository.AnalysisContentUnavailableException) {
                    if (!isFinishing && !isDestroyed) {
                        onAnalysisInvalidated()
                        Toast.makeText(this@AnalysisScopedActivity, R.string.analysis_result_unavailable, Toast.LENGTH_LONG).show()
                        finish()
                    }
                    return@launch
                }
                catch (_: Exception) {
                    if (!isFinishing && !isDestroyed) {
                        onAnalysisInvalidated()
                        Toast.makeText(this@AnalysisScopedActivity, R.string.analysis_storage_error, Toast.LENGTH_LONG).show()
                        finish()
                    }
                    return@launch
                }
            if (isFinishing || isDestroyed) return@launch
            val detail = if (result == null) null else try {
                app.getAnalysisDetailUseCase(result, if (resources.configuration.locales[0].language == "en") "en" else "vi")
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (_: Exception) { null }
            if (result == null || detail == null || repository.sessionKey() != session) {
                onAnalysisInvalidated()
                Toast.makeText(this@AnalysisScopedActivity, R.string.analysis_result_unavailable, Toast.LENGTH_LONG).show()
                finish()
            } else {
                validatedUnlockStatus = if (requiresUnlock || observesUnlock) try { app.analysisUnlockRepository.status(session, resultId) }
                    catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                    catch (_: Exception) { null } else null
                if (requiresUnlock && validatedUnlockStatus?.isUnlocked != true) {
                    onAnalysisInvalidated()
                    Toast.makeText(this@AnalysisScopedActivity, R.string.analysis_result_unavailable, Toast.LENGTH_LONG).show()
                    finish()
                    return@launch
                }
                // Validation can finish inside onResume, before the lifecycle reports RESUMED.
                lifecycle.withResumed {
                    onAnalysisValidated(result, detail)
                    if (!isFinishing) {
                        validated = true
                        window.decorView.alpha = 1f
                    }
                }
            }
        }
    }
    protected open fun onAnalysisValidated(identity: com.example.lagallens.domain.model.analysis.AnalysisIdentity, detail: com.example.lagallens.domain.model.analysis.AnalysisDetail) = Unit
    protected open fun onAnalysisInvalidated() = Unit

    override fun onPause() {
        validationJob?.cancel()
        validated = false
        window.decorView.alpha = 0f
        supportFragmentManager.fragments.filterIsInstance<androidx.fragment.app.DialogFragment>()
            .forEach { it.dismissAllowingStateLoss() }
        super.onPause()
    }
    override fun startActivity(target: Intent) {
        if (!validated) return
        val app = application as LegalLensApplication
        if (app.analysisIdentityRepository.sessionKey() != session) return
        if (target.component?.className?.startsWith("com.example.lagallens.presentation.feature.analysis.") == true) {
            target.putExtra(AnalysisProcessingActivity.EXTRA_RESULT_ID, resultId)
            target.putExtra(AnalysisProcessingActivity.EXTRA_SESSION_KEY, session)
        }
        super.startActivity(target)
    }
}
