package com.example.lagallens.presentation.feature.analysis.risk.detail.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.databinding.ActivityAnalysisRiskDetailBinding
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
        binding.analysisRiskDetailBack.setOnClickListener {
            viewModel.onEvent(AnalysisRiskDetailUiEvent.BackClicked)
        }
        binding.analysisRiskDetailLegalReference.setOnClickListener {
            viewModel.onEvent(AnalysisRiskDetailUiEvent.LegalReferenceClicked)
        }
        observeViewModel()
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
        analysisRiskDetailTitle.setText(state.titleRes)
        analysisRiskDetailAttentionReason.setText(state.attentionReasonRes)
        analysisRiskDetailClauseExcerpt.setText(state.clauseExcerptRes)
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
        fun createIntent(context: Context): Intent = Intent(context, AnalysisRiskDetailActivity::class.java)
    }
}
