package com.example.lagallens.presentation.feature.analysis.failure.ui

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
import com.example.lagallens.databinding.ActivityAnalysisFailureBinding
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import com.example.lagallens.presentation.feature.analysis.failure.contract.AnalysisFailureUiEffect
import com.example.lagallens.presentation.feature.analysis.failure.contract.AnalysisFailureUiEvent
import com.example.lagallens.presentation.feature.analysis.failure.viewmodel.AnalysisFailureViewModel
import com.example.lagallens.presentation.feature.analysis.processing.ui.AnalysisProcessingActivity
import com.example.lagallens.presentation.feature.main.ui.MainActivity
import kotlinx.coroutines.launch

class AnalysisFailureActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAnalysisFailureBinding
    private val viewModel by viewModels<AnalysisFailureViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityAnalysisFailureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val owner = intent.getStringExtra(AnalysisProcessingActivity.EXTRA_SESSION_KEY).orEmpty()
        val app = application as com.example.lagallens.LegalLensApplication
        if (owner.isBlank() || app.currentSessionKey() != owner) { binding.root.visibility = android.view.View.INVISIBLE; finish(); return }
        lifecycleScope.launch {
            app.observeSessionKeys().collect { current ->
                if (current != owner) { binding.root.visibility = android.view.View.INVISIBLE; finish() }
            }
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.analysisFailureRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        bindEvents()
        observeViewModel()
        viewModel.onEvent(
            AnalysisFailureUiEvent.ScreenOpened(
                documentName = intent.getStringExtra(EXTRA_DOCUMENT_NAME).orEmpty(),
                documentMetadata = intent.getStringExtra(EXTRA_DOCUMENT_METADATA).orEmpty()
            )
        )
    }

    private fun bindEvents() = with(binding) {
        listOf(analysisFailureBack, analysisFailureRetry, analysisFailureReturnHome).forEach { it.bindPressFeedback() }
        analysisFailureBack.setOnClickListener {
            viewModel.onEvent(AnalysisFailureUiEvent.BackClicked)
        }
        analysisFailureRetry.setOnClickListener {
            viewModel.onEvent(AnalysisFailureUiEvent.RetryClicked)
        }
        analysisFailureReturnHome.setOnClickListener {
            viewModel.onEvent(AnalysisFailureUiEvent.ReturnHomeClicked)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiEffect.collect(::handleEffect) }
            }
        }
    }

    private fun handleEffect(effect: AnalysisFailureUiEffect) {
        when (effect) {
            AnalysisFailureUiEffect.NavigateBack -> finish()
            AnalysisFailureUiEffect.NavigateToHome -> {
                startActivity(
                    Intent(this, MainActivity::class.java).addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                )
                finish()
            }

            is AnalysisFailureUiEffect.RetryAnalysis -> {
                if ((application as com.example.lagallens.LegalLensApplication).currentSessionKey() != intent.getStringExtra(AnalysisProcessingActivity.EXTRA_SESSION_KEY)) return
                startActivity(
                    AnalysisProcessingActivity.createIntent(
                        context = this,
                        documentName = effect.documentName,
                        documentMetadata = effect.documentMetadata
                    ).putExtra(AnalysisProcessingActivity.EXTRA_OPERATION_ID, intent.getStringExtra(AnalysisProcessingActivity.EXTRA_OPERATION_ID))
                        .putExtra(AnalysisProcessingActivity.EXTRA_SESSION_KEY, intent.getStringExtra(AnalysisProcessingActivity.EXTRA_SESSION_KEY))
                        .putExtra(com.example.lagallens.presentation.feature.analysis.start.ui.AnalysisStartActivity.EXTRA_CONTRACT_ID, intent.getStringExtra(com.example.lagallens.presentation.feature.analysis.start.ui.AnalysisStartActivity.EXTRA_CONTRACT_ID))
                        .putExtra(com.example.lagallens.presentation.feature.analysis.start.ui.AnalysisStartActivity.EXTRA_DOCUMENT_ID, intent.getStringExtra(com.example.lagallens.presentation.feature.analysis.start.ui.AnalysisStartActivity.EXTRA_DOCUMENT_ID))
                        .putExtra("analysis_return_home", intent.getBooleanExtra("analysis_return_home", false))
                )
                finish()
            }
        }
    }

    companion object {
        private const val EXTRA_DOCUMENT_NAME = "analysis_failure_document_name"
        private const val EXTRA_DOCUMENT_METADATA = "analysis_failure_document_metadata"

        fun createIntent(
            context: Context,
            documentName: String,
            documentMetadata: String
        ): Intent = Intent(context, AnalysisFailureActivity::class.java)
            .putExtra(EXTRA_DOCUMENT_NAME, documentName)
            .putExtra(EXTRA_DOCUMENT_METADATA, documentMetadata)
    }
}
