package com.example.lagallens.presentation.feature.analysis.risk.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.R
import com.example.lagallens.presentation.common.utils.isDemoData
import com.example.lagallens.databinding.ActivityAnalysisRiskOverviewBinding
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import com.example.lagallens.presentation.feature.analysis.risk.contract.AnalysisRiskOverviewUiEffect
import com.example.lagallens.presentation.feature.analysis.risk.contract.AnalysisRiskOverviewUiEvent
import com.example.lagallens.presentation.feature.analysis.risk.contract.AnalysisRiskOverviewUiState
import com.example.lagallens.presentation.feature.analysis.risk.findings.ui.AnalysisRiskFindingsActivity
import com.example.lagallens.presentation.feature.analysis.risk.viewmodel.AnalysisRiskOverviewViewModel
import kotlinx.coroutines.launch

class AnalysisRiskOverviewActivity : com.example.lagallens.presentation.feature.analysis.AnalysisScopedActivity() {
    override val requiresUnlock = false
    override val observesUnlock = true
    private lateinit var binding: ActivityAnalysisRiskOverviewBinding
    private val viewModel by viewModels<AnalysisRiskOverviewViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityAnalysisRiskOverviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (!isDemoData()) binding.analysisRiskOverviewDisclaimer.setText(R.string.analysis_risk_overview_disclaimer_real)
        ViewCompat.setOnApplyWindowInsetsListener(binding.analysisRiskOverviewRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        listOf(binding.analysisRiskOverviewBack, binding.analysisRiskOverviewViewDetails).forEach { it.bindPressFeedback() }
        binding.analysisRiskOverviewBack.setOnClickListener {
            viewModel.onEvent(AnalysisRiskOverviewUiEvent.BackClicked)
        }
        binding.analysisRiskOverviewViewDetails.setOnClickListener {
            viewModel.onEvent(AnalysisRiskOverviewUiEvent.ViewDetailsClicked)
        }
        observeViewModel()
        viewModel.onEvent(
            AnalysisRiskOverviewUiEvent.ScreenOpened(
                documentName = intent.getStringExtra(EXTRA_DOCUMENT_NAME).orEmpty(),
                documentMetadata = intent.getStringExtra(EXTRA_DOCUMENT_METADATA).orEmpty()
            )
        )
    }

    override fun onAnalysisValidated(identity: com.example.lagallens.domain.model.analysis.AnalysisIdentity, detail: com.example.lagallens.domain.model.analysis.AnalysisDetail) { viewModel.setResult(identity, detail, validatedUnlockStatus?.isUnlocked == true) }
    override fun onAnalysisInvalidated() { viewModel.clearResult() }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.uiEffect.collect(::handleEffect) }
            }
        }
    }

    private fun render(state: AnalysisRiskOverviewUiState) = with(binding) {
        analysisRiskOverviewScore.text = state.score?.toString() ?: getString(R.string.analysis_result_unavailable)
        analysisRiskOverviewScoreRing.progress = state.score ?: 0
        analysisRiskOverviewScoreRing.progressTintList = null
        analysisRiskOverviewSeverity.visibility = if (state.highestSeverity == null) android.view.View.GONE else android.view.View.VISIBLE
        state.highestSeverity?.let { level ->
            analysisRiskOverviewSeverity.setText(level.labelRes)
            analysisRiskOverviewSeverity.setTextColor(getColor(level.labelColorRes))
            analysisRiskOverviewScoreRing.progressTintList =
                android.content.res.ColorStateList.valueOf(getColor(level.labelColorRes))
            analysisRiskOverviewSeverity.setBackgroundResource(level.badgeBackgroundRes)
        }
        analysisRiskOverviewSummaryTitle.text = resources.getQuantityString(
            R.plurals.analysis_risk_overview_summary_title, state.totalRiskCount, state.totalRiskCount
        )
        analysisRiskOverviewLockedNote.isVisible = state.lockedRiskCount > 0
        analysisRiskOverviewLockedNote.text = resources.getQuantityString(
            R.plurals.analysis_risk_overview_locked_note,
            state.totalRiskCount,
            state.totalRiskCount - state.lockedRiskCount,
            state.totalRiskCount
        )
        analysisRiskOverviewHighRiskCount.text = state.highRiskCount.toString()
        analysisRiskOverviewMediumRiskCount.text = state.mediumRiskCount.toString()
        analysisRiskOverviewLowRiskCount.text = state.lowRiskCount.toString()
    }

    private fun handleEffect(effect: AnalysisRiskOverviewUiEffect) {
        when (effect) {
            AnalysisRiskOverviewUiEffect.NavigateBack -> finish()
            is AnalysisRiskOverviewUiEffect.NavigateToFindings -> startActivity(
                AnalysisRiskFindingsActivity.createIntent(
                    context = this,
                    documentName = effect.documentName,
                    documentMetadata = effect.documentMetadata
                )
            )
        }
    }

    companion object {
        private const val EXTRA_DOCUMENT_NAME = "analysis_risk_overview_document_name"
        private const val EXTRA_DOCUMENT_METADATA = "analysis_risk_overview_document_metadata"

        fun createIntent(
            context: Context,
            documentName: String,
            documentMetadata: String
        ): Intent = Intent(context, AnalysisRiskOverviewActivity::class.java)
            .putExtra(EXTRA_DOCUMENT_NAME, documentName)
            .putExtra(EXTRA_DOCUMENT_METADATA, documentMetadata)
    }
}
