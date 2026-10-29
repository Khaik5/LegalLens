package com.example.lagallens.presentation.feature.analysis.start.ui

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
import com.example.lagallens.databinding.ActivityAnalysisStartBinding
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import com.example.lagallens.presentation.feature.analysis.start.adapter.AnalysisStartBenefitAdapter
import com.example.lagallens.presentation.feature.analysis.start.adapter.AnalysisStartBenefitSpacingDecoration
import com.example.lagallens.presentation.feature.analysis.start.contract.AnalysisStartUiEffect
import com.example.lagallens.presentation.feature.analysis.start.contract.AnalysisStartUiEvent
import com.example.lagallens.presentation.feature.analysis.start.contract.AnalysisStartUiState
import com.example.lagallens.presentation.feature.analysis.start.viewmodel.AnalysisStartViewModel
import com.example.lagallens.presentation.feature.analysis.processing.ui.AnalysisProcessingActivity
import kotlinx.coroutines.launch

class AnalysisStartActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAnalysisStartBinding
    private val viewModel by viewModels<AnalysisStartViewModel>()
    private val benefitAdapter = AnalysisStartBenefitAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityAnalysisStartBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.analysisStartRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        bindEvents()
        bindBenefits()
        observeViewModel()
        viewModel.onEvent(
            AnalysisStartUiEvent.ScreenOpened(
                documentName = intent.getStringExtra(EXTRA_DOCUMENT_NAME)
                    .orEmpty()
                    .ifBlank { getString(R.string.analysis_start_document_name) },
                documentMetadata = intent.getStringExtra(EXTRA_DOCUMENT_METADATA)
                    .orEmpty()
                    .ifBlank { getString(R.string.analysis_start_document_metadata) }
            )
        )
    }

    private fun bindEvents() = with(binding) {
        listOf(analysisStartBack, analysisStartAction).forEach { it.bindPressFeedback() }
        analysisStartBack.setOnClickListener {
            viewModel.onEvent(AnalysisStartUiEvent.BackClicked)
        }
        analysisStartAction.setOnClickListener {
            viewModel.onEvent(AnalysisStartUiEvent.StartAnalysisClicked)
        }
    }

    private fun bindBenefits() = with(binding.analysisStartBenefits) {
        layoutManager = LinearLayoutManager(this@AnalysisStartActivity)
        adapter = benefitAdapter
        addItemDecoration(
            AnalysisStartBenefitSpacingDecoration(
                resources.getDimensionPixelSize(R.dimen.analysis_benefit_card_spacing)
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

    private fun render(state: AnalysisStartUiState) = with(binding) {
        analysisStartDocumentName.text = state.documentName
        analysisStartDocumentMetadata.text = state.documentMetadata
        benefitAdapter.submitList(state.benefits)
    }

    private fun handleEffect(effect: AnalysisStartUiEffect) {
        when (effect) {
            AnalysisStartUiEffect.NavigateBack -> finish()
            is AnalysisStartUiEffect.NavigateToAnalysisProcessing -> {
                startActivity(
                    AnalysisProcessingActivity.createIntent(
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
        private const val EXTRA_DOCUMENT_NAME = "analysis_start_document_name"
        private const val EXTRA_DOCUMENT_METADATA = "analysis_start_document_metadata"

        fun createIntent(
            context: Context,
            documentName: String,
            documentMetadata: String = context.getString(R.string.analysis_start_document_metadata)
        ): Intent = Intent(context, AnalysisStartActivity::class.java)
            .putExtra(EXTRA_DOCUMENT_NAME, documentName)
            .putExtra(EXTRA_DOCUMENT_METADATA, documentMetadata)
    }
}
