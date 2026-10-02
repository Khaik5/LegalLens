package com.example.lagallens.presentation.feature.analysis.risk.citation.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityAnalysisRiskCitationBinding
import com.example.lagallens.presentation.feature.analysis.risk.citation.contract.AnalysisRiskCitationUiEffect
import com.example.lagallens.presentation.feature.analysis.risk.citation.contract.AnalysisRiskCitationUiEvent
import com.example.lagallens.presentation.feature.analysis.risk.citation.contract.AnalysisRiskCitationUiState
import com.example.lagallens.presentation.feature.analysis.risk.citation.viewmodel.AnalysisRiskCitationViewModel
import kotlinx.coroutines.launch

class AnalysisRiskCitationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAnalysisRiskCitationBinding
    private val viewModel by viewModels<AnalysisRiskCitationViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityAnalysisRiskCitationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.analysisRiskCitationRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.analysisRiskCitationBack.setOnClickListener {
            viewModel.onEvent(AnalysisRiskCitationUiEvent.BackClicked)
        }
        binding.analysisRiskCitationReturn.setOnClickListener {
            viewModel.onEvent(AnalysisRiskCitationUiEvent.ReturnToRiskClicked)
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

    private fun render(state: AnalysisRiskCitationUiState) = with(binding) {
        analysisRiskCitationNotice.setText(state.noticeRes)
        analysisRiskCitationFindingLabel.setText(state.findingLabelRes)
        analysisRiskCitationFindingTitle.setText(state.findingTitleRes)
        analysisRiskCitationFindingSource.setText(state.findingSourceRes)
        analysisRiskCitationHeading.setText(state.citationHeadingRes)
        analysisRiskCitationLegalReference.setText(state.legalReferenceRes)
        analysisRiskCitationBody.text = createHighlightedCitation(
            body = getString(state.citationBodyRes),
            highlight = getString(state.citationHighlightRes)
        )
    }

    private fun createHighlightedCitation(body: String, highlight: String): SpannableString {
        val styledBody = SpannableString(body)
        val start = body.indexOf(highlight)
        if (start < 0) return styledBody
        val end = start + highlight.length
        styledBody.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this, R.color.contract_risk_high)),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        styledBody.setSpan(
            StyleSpan(Typeface.BOLD),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return styledBody
    }

    private fun handleEffect(effect: AnalysisRiskCitationUiEffect) {
        when (effect) {
            AnalysisRiskCitationUiEffect.NavigateBack -> finish()
        }
    }

    companion object {
        fun createIntent(context: Context): Intent = Intent(context, AnalysisRiskCitationActivity::class.java)
    }
}
