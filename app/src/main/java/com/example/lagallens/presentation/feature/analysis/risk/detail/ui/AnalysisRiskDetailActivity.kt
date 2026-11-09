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

class AnalysisRiskDetailActivity : com.example.lagallens.presentation.feature.analysis.AnalysisScopedActivity() {
    override val requiresUnlock = false
    override val observesUnlock = true
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

    }

    override fun onAnalysisInvalidated() { viewModel.clearResult() }

    override fun onAnalysisValidated(identity: com.example.lagallens.domain.model.analysis.AnalysisIdentity, detail: com.example.lagallens.domain.model.analysis.AnalysisDetail) {
        val id = intent.getStringExtra(EXTRA_FINDING_ID)
        val count = com.example.lagallens.domain.model.analysis.AnalysisPreview.visibleCount(detail.findings.size, validatedUnlockStatus?.isUnlocked == true)
        if (id == null || detail.findings.take(count).none { it.id == id } || !viewModel.loadFinding(id, identity, detail)) {
            android.widget.Toast.makeText(this, R.string.analysis_locked_item_unavailable, android.widget.Toast.LENGTH_LONG).show()
            finish()
            return
        }
        render(viewModel.uiState.value)
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
        analysisRiskDetailTitle.text = state.title
        analysisRiskDetailTitle.text = getString(R.string.analysis_snapshot_demo_metadata, analysisRiskDetailTitle.text)
        analysisRiskDetailAttentionReason.text = state.attentionReason
        analysisRiskDetailClauseExcerpt.text = state.clauseExcerpt
        analysisRiskDetailClauseReference.text = state.clauseReference
        analysisRiskDetailClauseHeading.setText(if (state.isDetailedExample) R.string.analysis_risk_detail_clause_heading else R.string.analysis_risk_finding_summary)
        analysisRiskDetailLegalHeading.isVisible = state.legalExplanation != null
        analysisRiskDetailLegalExplanation.isVisible = state.legalExplanation != null
        analysisRiskDetailLegalReference.isVisible = false
        analysisRiskDetailRecommendationCard.isVisible = state.recommendation != null
        state.legalExplanation?.let { analysisRiskDetailLegalExplanation.text = it }
        analysisRiskDetailLegalReferenceTitle.setText(state.legalReferenceTitleRes)
        analysisRiskDetailLegalReferenceSubtitle.setText(state.legalReferenceSubtitleRes)
        state.recommendation?.let { analysisRiskDetailRecommendation.text = it }
    }

    private fun handleEffect(effect: AnalysisRiskDetailUiEffect) {
        when (effect) {
            AnalysisRiskDetailUiEffect.NavigateBack -> finish()
            AnalysisRiskDetailUiEffect.CitationUnavailable -> android.widget.Toast.makeText(this, R.string.analysis_citation_unavailable, android.widget.Toast.LENGTH_LONG).show()
            AnalysisRiskDetailUiEffect.NavigateToCitation -> startActivity(
                AnalysisRiskCitationActivity.createIntent(this)
            )
        }
    }

    companion object {
        private const val EXTRA_FINDING_ID = "analysis_risk_detail_finding_id"
        fun createIntent(context: Context, findingId: String = ""): Intent =
            Intent(context, AnalysisRiskDetailActivity::class.java).putExtra(EXTRA_FINDING_ID, findingId)
    }
}
