package com.example.lagallens.presentation.feature.analysis.risk.detail.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.example.lagallens.R
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.databinding.ActivityAnalysisRiskDetailBinding
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import com.example.lagallens.presentation.feature.analysis.risk.citation.ui.AnalysisRiskCitationActivity
import com.example.lagallens.presentation.feature.analysis.risk.detail.contract.AnalysisRiskDetailUiEffect
import com.example.lagallens.presentation.feature.analysis.risk.detail.contract.AnalysisRiskDetailUiEvent
import com.example.lagallens.presentation.feature.analysis.risk.detail.contract.AnalysisRiskDetailUiState
import com.example.lagallens.presentation.feature.analysis.risk.detail.viewmodel.AnalysisRiskDetailViewModel
import kotlinx.coroutines.launch

class AnalysisRiskDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAnalysisRiskDetailBinding
    private val viewModel by viewModels<AnalysisRiskDetailViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityAnalysisRiskDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.analysisRiskDetailRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        listOf(binding.analysisRiskDetailBack, binding.analysisRiskDetailLegalReference).forEach { it.bindPressFeedback() }
        binding.analysisRiskDetailBack.setOnClickListener {
            viewModel.onEvent(AnalysisRiskDetailUiEvent.BackClicked)
        }
        binding.analysisRiskDetailLegalReference.setOnClickListener {
            viewModel.onEvent(AnalysisRiskDetailUiEvent.LegalReferenceClicked)
        }
        observeViewModel()
        viewModel.loadFinding(intent.getStringExtra(EXTRA_FINDING_ID) ?: "penalty-cap")
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.uiEffect.collect(::handleEffect) }
            }
        }
    }

    private fun render(state: AnalysisRiskDetailUiState) = with(binding) {
        analysisRiskDetailSeverity.setText(state.severityRes)
        analysisRiskDetailSeverity.setBackgroundResource(state.severityBackgroundRes)
        analysisRiskDetailSeverity.setTextColor(getColor(state.severityColorRes))
        analysisRiskDetailTitle.setText(state.titleRes)
        analysisRiskDetailAttentionReason.setText(state.attentionReasonRes)
        analysisRiskDetailClauseExcerpt.setText(state.clauseExcerptRes)
        analysisRiskDetailClauseReference.setText(state.clauseReferenceRes)
        analysisRiskDetailClauseHeading.setText(if (state.isDetailedExample) R.string.analysis_risk_detail_clause_heading else R.string.analysis_risk_finding_summary)
        analysisRiskDetailLegalHeading.isVisible = state.isDetailedExample
        analysisRiskDetailLegalExplanation.isVisible = state.isDetailedExample
        analysisRiskDetailLegalReference.isVisible = state.isDetailedExample
        analysisRiskDetailRecommendationCard.isVisible = state.isDetailedExample
        analysisRiskDetailLegalExplanation.setText(state.legalExplanationRes)
        analysisRiskDetailLegalReferenceTitle.setText(state.legalReferenceTitleRes)
        analysisRiskDetailLegalReferenceSubtitle.setText(state.legalReferenceSubtitleRes)
        analysisRiskDetailRecommendation.setText(state.recommendationRes)
    }

    private fun handleEffect(effect: AnalysisRiskDetailUiEffect) {
        when (effect) {
            AnalysisRiskDetailUiEffect.NavigateBack -> finish()
            AnalysisRiskDetailUiEffect.NavigateToCitation -> startActivity(
                AnalysisRiskCitationActivity.createIntent(this)
            )
        }
    }

    companion object {
        private const val EXTRA_FINDING_ID = "analysis_risk_detail_finding_id"
        fun createIntent(context: Context, findingId: String = "penalty-cap"): Intent =
            Intent(context, AnalysisRiskDetailActivity::class.java).putExtra(EXTRA_FINDING_ID, findingId)
    }
}
