package com.example.lagallens.presentation.feature.analysis.processing.ui

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
import com.example.lagallens.databinding.ActivityAnalysisProcessingBinding
import com.example.lagallens.presentation.common.adapter.ProcessingTimelineStepAdapter
import com.example.lagallens.presentation.feature.analysis.processing.contract.AnalysisProcessingUiEffect
import com.example.lagallens.presentation.feature.analysis.processing.contract.AnalysisProcessingUiEvent
import com.example.lagallens.presentation.feature.analysis.processing.contract.AnalysisProcessingUiState
import com.example.lagallens.presentation.feature.analysis.processing.viewmodel.AnalysisProcessingViewModel
import com.example.lagallens.presentation.feature.analysis.failure.ui.AnalysisFailureActivity
import com.example.lagallens.presentation.feature.analysis.result.ui.AnalysisResultActivity
import kotlinx.coroutines.launch

class AnalysisProcessingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAnalysisProcessingBinding
    private val viewModel by viewModels<AnalysisProcessingViewModel>()
    private val stepAdapter = ProcessingTimelineStepAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityAnalysisProcessingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.analysisProcessingRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        bindTimeline()
        observeViewModel()
        viewModel.onEvent(
            AnalysisProcessingUiEvent.ScreenOpened(
                documentName = intent.getStringExtra(EXTRA_DOCUMENT_NAME)
                    .orEmpty()
                    .ifBlank { getString(R.string.analysis_start_document_name) },
                documentMetadata = intent.getStringExtra(EXTRA_DOCUMENT_METADATA)
                    .orEmpty()
                    .ifBlank { getString(R.string.analysis_start_document_metadata) },
                shouldSimulateFailure = intent.getBooleanExtra(EXTRA_SHOULD_SIMULATE_FAILURE, false)
            )
        )
    }

    private fun bindTimeline() = with(binding.analysisProcessingTimeline) {
        layoutManager = LinearLayoutManager(this@AnalysisProcessingActivity)
        adapter = stepAdapter
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.uiEffect.collect(::handleEffect) }
            }
        }
    }

    private fun render(state: AnalysisProcessingUiState) = with(binding) {
        analysisProcessingDocumentName.text = state.documentName
        analysisProcessingDocumentMetadata.text = state.documentMetadata
        stepAdapter.submitList(state.steps)
    }

    private fun handleEffect(effect: AnalysisProcessingUiEffect) {
        when (effect) {
            is AnalysisProcessingUiEffect.NavigateToAnalysisResult -> {
                startActivity(
                    AnalysisResultActivity.createIntent(
                        context = this,
                        documentName = effect.documentName,
                        documentMetadata = effect.documentMetadata
                    ).putExtra("analysis_return_home", intent.getBooleanExtra("analysis_return_home", false))
                )
                finish()
            }

            is AnalysisProcessingUiEffect.NavigateToAnalysisFailure -> {
                startActivity(
                    AnalysisFailureActivity.createIntent(
                        context = this,
                        documentName = effect.documentName,
                        documentMetadata = effect.documentMetadata
                    ).putExtra("analysis_return_home", intent.getBooleanExtra("analysis_return_home", false))
                )
                finish()
            }
        }
    }

    companion object {
        private const val EXTRA_DOCUMENT_NAME = "analysis_processing_document_name"
        private const val EXTRA_DOCUMENT_METADATA = "analysis_processing_document_metadata"
        private const val EXTRA_SHOULD_SIMULATE_FAILURE = "analysis_should_simulate_failure"

        fun createIntent(
            context: Context,
            documentName: String,
            documentMetadata: String,
            shouldSimulateFailure: Boolean = false
        ): Intent = Intent(context, AnalysisProcessingActivity::class.java)
            .putExtra(EXTRA_DOCUMENT_NAME, documentName)
            .putExtra(EXTRA_DOCUMENT_METADATA, documentMetadata)
            .putExtra(EXTRA_SHOULD_SIMULATE_FAILURE, shouldSimulateFailure)
    }
}
