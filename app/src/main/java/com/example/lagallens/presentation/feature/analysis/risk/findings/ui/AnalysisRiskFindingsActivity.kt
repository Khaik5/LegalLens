package com.example.lagallens.presentation.feature.analysis.risk.findings.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityAnalysisRiskFindingsBinding
import com.example.lagallens.presentation.feature.analysis.risk.findings.adapter.AnalysisRiskFindingsAdapter
import com.example.lagallens.presentation.feature.analysis.risk.findings.contract.AnalysisRiskFindingsUiEffect
import com.example.lagallens.presentation.feature.analysis.risk.findings.contract.AnalysisRiskFindingsUiEvent
import com.example.lagallens.presentation.feature.analysis.risk.findings.contract.AnalysisRiskFindingsUiState
import com.example.lagallens.presentation.feature.analysis.risk.findings.model.AnalysisRiskFilter
import com.example.lagallens.presentation.feature.analysis.risk.findings.viewmodel.AnalysisRiskFindingsViewModel
import com.example.lagallens.presentation.feature.analysis.risk.detail.ui.AnalysisRiskDetailActivity
import kotlinx.coroutines.launch

class AnalysisRiskFindingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAnalysisRiskFindingsBinding
    private val viewModel by viewModels<AnalysisRiskFindingsViewModel>()
    private val findingsAdapter = AnalysisRiskFindingsAdapter { findingId ->
        viewModel.onEvent(AnalysisRiskFindingsUiEvent.FindingClicked(findingId))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityAnalysisRiskFindingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.analysisRiskFindingsRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.analysisRiskFindingsBack.setOnClickListener {
            viewModel.onEvent(AnalysisRiskFindingsUiEvent.BackClicked)
        }
        binding.analysisRiskFindingsItems.apply {
            layoutManager = LinearLayoutManager(this@AnalysisRiskFindingsActivity)
            adapter = findingsAdapter
        }
        bindFilters()
        observeViewModel()
        viewModel.onEvent(
            AnalysisRiskFindingsUiEvent.ScreenOpened(
                documentName = intent.getStringExtra(EXTRA_DOCUMENT_NAME).orEmpty(),
                documentMetadata = intent.getStringExtra(EXTRA_DOCUMENT_METADATA).orEmpty()
            )
        )
    }

    private fun bindFilters() = with(binding) {
        analysisRiskFindingsFilterAll.setOnClickListener {
            viewModel.onEvent(AnalysisRiskFindingsUiEvent.FilterClicked(AnalysisRiskFilter.ALL))
        }
        analysisRiskFindingsFilterHigh.setOnClickListener {
            viewModel.onEvent(AnalysisRiskFindingsUiEvent.FilterClicked(AnalysisRiskFilter.HIGH))
        }
        analysisRiskFindingsFilterMedium.setOnClickListener {
            viewModel.onEvent(AnalysisRiskFindingsUiEvent.FilterClicked(AnalysisRiskFilter.MEDIUM))
        }
        analysisRiskFindingsFilterLow.setOnClickListener {
            viewModel.onEvent(AnalysisRiskFindingsUiEvent.FilterClicked(AnalysisRiskFilter.LOW))
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.uiEffect.collect(::handleEffect) }
            }
        }
    }

    private fun render(state: AnalysisRiskFindingsUiState) = with(binding) {
        findingsAdapter.submitList(state.findings)
        setFilterAppearance(analysisRiskFindingsFilterAll, state.selectedFilter == AnalysisRiskFilter.ALL)
        setFilterAppearance(analysisRiskFindingsFilterHigh, state.selectedFilter == AnalysisRiskFilter.HIGH)
        setFilterAppearance(analysisRiskFindingsFilterMedium, state.selectedFilter == AnalysisRiskFilter.MEDIUM)
        setFilterAppearance(analysisRiskFindingsFilterLow, state.selectedFilter == AnalysisRiskFilter.LOW)
    }

    private fun setFilterAppearance(filter: android.widget.TextView, selected: Boolean) {
        filter.setBackgroundResource(
            if (selected) R.drawable.bg_analysis_risk_findings_filter_selected
            else R.drawable.bg_analysis_risk_findings_filter_unselected
        )
        filter.setTextColor(getColor(if (selected) R.color.white else R.color.legal_lens_status_text))
    }

    private fun handleEffect(effect: AnalysisRiskFindingsUiEffect) {
        when (effect) {
            AnalysisRiskFindingsUiEffect.NavigateBack -> finish()
            AnalysisRiskFindingsUiEffect.NavigateToRiskDetail -> startActivity(
                AnalysisRiskDetailActivity.createIntent(this)
            )
            is AnalysisRiskFindingsUiEffect.ShowMessage -> {
                Toast.makeText(this, effect.messageRes, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        binding.analysisRiskFindingsItems.adapter = null
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_DOCUMENT_NAME = "analysis_risk_findings_document_name"
        private const val EXTRA_DOCUMENT_METADATA = "analysis_risk_findings_document_metadata"

        fun createIntent(
            context: Context,
            documentName: String,
            documentMetadata: String
        ): Intent = Intent(context, AnalysisRiskFindingsActivity::class.java)
            .putExtra(EXTRA_DOCUMENT_NAME, documentName)
            .putExtra(EXTRA_DOCUMENT_METADATA, documentMetadata)
    }
}
