package com.example.lagallens.presentation.feature.analysis.summary.ui

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityAnalysisSummaryBinding
import com.example.lagallens.presentation.feature.analysis.keyfacts.ui.AnalysisKeyFactsActivity
import com.example.lagallens.presentation.feature.analysis.summary.adapter.AnalysisSummaryAdapter
import com.example.lagallens.presentation.feature.analysis.summary.contract.AnalysisSummaryUiEffect
import com.example.lagallens.presentation.feature.analysis.summary.contract.AnalysisSummaryUiEvent
import com.example.lagallens.presentation.feature.analysis.summary.contract.AnalysisSummaryUiState
import com.example.lagallens.presentation.feature.analysis.summary.viewmodel.AnalysisSummaryViewModel
import kotlinx.coroutines.launch

class AnalysisSummaryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAnalysisSummaryBinding
    private val viewModel by viewModels<AnalysisSummaryViewModel>()
    private val summaryAdapter = AnalysisSummaryAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityAnalysisSummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.analysisSummaryRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.analysisSummaryBack.setOnClickListener {
            viewModel.onEvent(AnalysisSummaryUiEvent.BackClicked)
        }
        binding.analysisSummaryImportantInformation.setOnClickListener {
            viewModel.onEvent(AnalysisSummaryUiEvent.ImportantInformationClicked)
        }
        binding.analysisSummarySections.apply {
            layoutManager = LinearLayoutManager(this@AnalysisSummaryActivity)
            adapter = summaryAdapter
            isNestedScrollingEnabled = false
        }
        observeViewModel()
        viewModel.onEvent(
            AnalysisSummaryUiEvent.ScreenOpened(
                documentName = intent.getStringExtra(EXTRA_DOCUMENT_NAME)
                    .orEmpty()
                    .ifBlank { getString(R.string.analysis_start_document_name) },
                documentMetadata = intent.getStringExtra(EXTRA_DOCUMENT_METADATA)
                    .orEmpty()
                    .ifBlank { getString(R.string.analysis_start_document_metadata) }
            )
        )
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.uiEffect.collect(::handleEffect) }
            }
        }
    }

    private fun render(state: AnalysisSummaryUiState) = with(binding) {
        analysisSummaryDocumentName.text = state.documentName
        analysisSummaryDocumentMetadata.text = state.documentMetadata
        summaryAdapter.submitList(state.sections)
    }

    private fun handleEffect(effect: AnalysisSummaryUiEffect) {
        when (effect) {
            AnalysisSummaryUiEffect.NavigateBack -> finish()
            is AnalysisSummaryUiEffect.NavigateToKeyFacts -> startActivity(
                AnalysisKeyFactsActivity.createIntent(
                    context = this,
                    documentName = effect.documentName,
                    documentMetadata = effect.documentMetadata
                )
            )
        }
    }

    override fun onDestroy() {
        binding.analysisSummarySections.adapter = null
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_DOCUMENT_NAME = "analysis_summary_document_name"
        private const val EXTRA_DOCUMENT_METADATA = "analysis_summary_document_metadata"

        fun createIntent(
            context: Context,
            documentName: String,
            documentMetadata: String
        ): Intent = Intent(context, AnalysisSummaryActivity::class.java)
            .putExtra(EXTRA_DOCUMENT_NAME, documentName)
            .putExtra(EXTRA_DOCUMENT_METADATA, documentMetadata)
    }
}
