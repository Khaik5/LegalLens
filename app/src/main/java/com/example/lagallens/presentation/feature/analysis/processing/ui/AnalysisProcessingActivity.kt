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
import com.example.lagallens.presentation.feature.analysis.start.ui.AnalysisStartActivity
import kotlinx.coroutines.launch

class AnalysisProcessingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAnalysisProcessingBinding
    private val viewModel by viewModels<AnalysisProcessingViewModel> {
        val app = application as com.example.lagallens.LegalLensApplication
        AnalysisProcessingViewModel.Factory(app.analysisIdentityRepository, app.analysisRepository)
    }
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
        val owner = intent.getStringExtra(EXTRA_SESSION_KEY).orEmpty()
        val app = application as com.example.lagallens.LegalLensApplication
        if (owner.isBlank() || app.currentSessionKey() != owner) { binding.root.visibility = android.view.View.INVISIBLE; viewModel.invalidateSession(); finish(); return }
        lifecycleScope.launch {
            app.observeSessionKeys().collect { current ->
                if (current != owner) { binding.root.visibility = android.view.View.INVISIBLE; viewModel.invalidateSession(); finish() }
            }
        }
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
                    .orEmpty(),
                documentMetadata = intent.getStringExtra(EXTRA_DOCUMENT_METADATA)
                    .orEmpty(),
                operationId = intent.getStringExtra(EXTRA_OPERATION_ID).orEmpty(),
                contractId = intent.getStringExtra(AnalysisStartActivity.EXTRA_CONTRACT_ID).orEmpty(),
                documentId = intent.getStringExtra(AnalysisStartActivity.EXTRA_DOCUMENT_ID).orEmpty(),
                sessionKey = intent.getStringExtra(EXTRA_SESSION_KEY).orEmpty()
            )
        )
    }

    private fun bindTimeline() = with(binding.analysisProcessingTimeline) {
        layoutManager = LinearLayoutManager(this@AnalysisProcessingActivity)
        adapter = stepAdapter
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch { viewModel.uiState.collect(::render) }
            }
        }
    }

    private fun render(state: AnalysisProcessingUiState) = with(binding) {
        analysisProcessingDocumentName.text = state.documentName
        analysisProcessingDocumentMetadata.text = state.documentMetadata
        stepAdapter.submitList(state.steps)
        state.completion?.let { completion ->
            if ((application as com.example.lagallens.LegalLensApplication).currentSessionKey() == intent.getStringExtra(EXTRA_SESSION_KEY)) {
                handleEffect(completion)
                viewModel.consumeCompletion(completion)
            }
        }
        Unit
    }

    private fun handleEffect(effect: AnalysisProcessingUiEffect) {
        when (effect) {
            is AnalysisProcessingUiEffect.NavigateToAnalysisResult -> {
                if ((application as com.example.lagallens.LegalLensApplication).currentSessionKey() != effect.sessionKey) return
                startActivity(
                    AnalysisResultActivity.createIntent(
                        context = this,
                        documentName = effect.documentName,
                        documentMetadata = effect.documentMetadata
                    ).putExtra(EXTRA_RESULT_ID, effect.resultId)
                        .putExtra(EXTRA_SESSION_KEY, effect.sessionKey)
                        .putExtra(AnalysisStartActivity.EXTRA_CONTRACT_ID, intent.getStringExtra(AnalysisStartActivity.EXTRA_CONTRACT_ID))
                        .putExtra(AnalysisStartActivity.EXTRA_DOCUMENT_ID, intent.getStringExtra(AnalysisStartActivity.EXTRA_DOCUMENT_ID))
                        .putExtra("analysis_return_home", intent.getBooleanExtra("analysis_return_home", false))
                )
                finish()
            }

            is AnalysisProcessingUiEffect.NavigateToAnalysisFailure -> {
                if ((application as com.example.lagallens.LegalLensApplication).currentSessionKey() != intent.getStringExtra(EXTRA_SESSION_KEY)) return
                startActivity(
                    AnalysisFailureActivity.createIntent(
                        context = this,
                        documentName = effect.documentName,
                        documentMetadata = effect.documentMetadata
                    ).putExtra(EXTRA_OPERATION_ID, intent.getStringExtra(EXTRA_OPERATION_ID))
                        .putExtra(EXTRA_SESSION_KEY, intent.getStringExtra(EXTRA_SESSION_KEY))
                        .putExtra(AnalysisStartActivity.EXTRA_CONTRACT_ID, intent.getStringExtra(AnalysisStartActivity.EXTRA_CONTRACT_ID))
                        .putExtra(AnalysisStartActivity.EXTRA_DOCUMENT_ID, intent.getStringExtra(AnalysisStartActivity.EXTRA_DOCUMENT_ID))
                        .putExtra("analysis_return_home", intent.getBooleanExtra("analysis_return_home", false))
                )
                finish()
            }
        }
    }

    companion object {
        const val EXTRA_RESULT_ID = "analysis_result_id"
        const val EXTRA_OPERATION_ID = "analysis_operation_id"
        const val EXTRA_SESSION_KEY = "analysis_session_key"
        private const val EXTRA_DOCUMENT_NAME = "analysis_processing_document_name"
        private const val EXTRA_DOCUMENT_METADATA = "analysis_processing_document_metadata"

        fun createIntent(
            context: Context,
            documentName: String,
            documentMetadata: String
        ): Intent = Intent(context, AnalysisProcessingActivity::class.java)
            .putExtra(EXTRA_DOCUMENT_NAME, documentName)
            .putExtra(EXTRA_DOCUMENT_METADATA, documentMetadata)
    }
}
