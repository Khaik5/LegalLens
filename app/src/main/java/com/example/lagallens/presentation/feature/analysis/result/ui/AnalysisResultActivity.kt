package com.example.lagallens.presentation.feature.analysis.result.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import com.example.lagallens.presentation.feature.main.ui.MainActivity
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
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityAnalysisResultBinding
import com.example.lagallens.presentation.feature.analysis.legalreferences.ui.AnalysisLegalReferencesActivity
import com.example.lagallens.presentation.feature.analysis.obligations.ui.AnalysisObligationsActivity
import com.example.lagallens.presentation.feature.analysis.parties.ui.AnalysisPartiesActivity
import com.example.lagallens.presentation.feature.analysis.risk.ui.AnalysisRiskOverviewActivity
import com.example.lagallens.presentation.feature.analysis.result.adapter.AnalysisResultAdapter
import com.example.lagallens.presentation.feature.analysis.result.contract.AnalysisResultUiEffect
import com.example.lagallens.presentation.feature.analysis.result.contract.AnalysisResultUiEvent
import com.example.lagallens.presentation.feature.analysis.result.contract.AnalysisResultUiState
import com.example.lagallens.presentation.feature.analysis.keyfacts.ui.AnalysisKeyFactsActivity
import com.example.lagallens.presentation.feature.analysis.summary.ui.AnalysisSummaryActivity
import com.example.lagallens.presentation.feature.analysis.result.viewmodel.AnalysisResultViewModel
import kotlinx.coroutines.launch

class AnalysisResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAnalysisResultBinding
    private val viewModel by viewModels<AnalysisResultViewModel> {
        AnalysisResultViewModel.Factory(
            (application as LegalLensApplication).getAnalysisOverviewUseCase
        )
    }
    private val resultAdapter = AnalysisResultAdapter { destination ->
        viewModel.onEvent(AnalysisResultUiEvent.ItemClicked(destination))
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
        binding = ActivityAnalysisResultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.analysisResultRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = returnFromResult()
        })
        binding.analysisResultBack.setOnClickListener {
            viewModel.onEvent(AnalysisResultUiEvent.BackClicked)
        }
        binding.analysisResultItems.apply {
            layoutManager = LinearLayoutManager(this@AnalysisResultActivity)
            adapter = resultAdapter
            isNestedScrollingEnabled = false
        }
        observeViewModel()
        viewModel.onEvent(
            AnalysisResultUiEvent.ScreenOpened(
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

    private fun render(state: AnalysisResultUiState) = with(binding) {
        analysisResultDocumentName.text = state.documentName
        analysisResultDocumentMetadata.text = state.errorMessageRes?.let { getString(it) } ?: state.documentMetadata
        analysisResultScore.text = state.score.toString()
        analysisResultKeyFactsValue.text = state.keyFactsCount.toString()
        analysisResultRiskValue.text = state.riskCount.toString()
        analysisResultReferenceValue.text = state.referenceCount.toString()
        // Each destination row has a fixed resource height and bottom margin. The
        // surrounding ScrollView must see the height of every row, not only the
        // RecyclerView's first viewport, otherwise the final row is unreachable.
        val rowHeight = resources.getDimensionPixelSize(R.dimen.analysis_result_item_height) +
            resources.getDimensionPixelSize(R.dimen.analysis_result_item_spacing)
        val listHeight = rowHeight * state.items.size
        if (analysisResultItems.layoutParams.height != listHeight) {
            analysisResultItems.layoutParams = analysisResultItems.layoutParams.apply { height = listHeight }
        }
        resultAdapter.submitList(state.items)
    }

    private fun handleEffect(effect: AnalysisResultUiEffect) {
        when (effect) {
            AnalysisResultUiEffect.NavigateBack -> returnFromResult()
            is AnalysisResultUiEffect.NavigateToSummary -> startActivity(
                AnalysisSummaryActivity.createIntent(
                    context = this,
                    documentName = effect.documentName,
                    documentMetadata = effect.documentMetadata
                )
            )

            is AnalysisResultUiEffect.NavigateToKeyFacts -> startActivity(
                AnalysisKeyFactsActivity.createIntent(
                    context = this,
                    documentName = effect.documentName,
                    documentMetadata = effect.documentMetadata
                )
            )

            is AnalysisResultUiEffect.NavigateToParties -> startActivity(
                AnalysisPartiesActivity.createIntent(
                    context = this,
                    documentName = effect.documentName,
                    documentMetadata = effect.documentMetadata
                )
            )

            is AnalysisResultUiEffect.NavigateToObligations -> startActivity(
                AnalysisObligationsActivity.createIntent(
                    context = this,
                    documentName = effect.documentName,
                    documentMetadata = effect.documentMetadata
                )
            )

            is AnalysisResultUiEffect.NavigateToRiskOverview -> startActivity(
                AnalysisRiskOverviewActivity.createIntent(
                    context = this,
                    documentName = effect.documentName,
                    documentMetadata = effect.documentMetadata
                )
            )

            is AnalysisResultUiEffect.NavigateToLegalReferences -> startActivity(
                AnalysisLegalReferencesActivity.createIntent(
                    context = this,
                    documentName = effect.documentName,
                    documentMetadata = effect.documentMetadata
                )
            )
        }
    }

    private fun returnFromResult() {
        if (intent.getBooleanExtra("analysis_return_home", false)) {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        }
        finish()
    }

    companion object {
        private const val EXTRA_DOCUMENT_NAME = "analysis_result_document_name"
        private const val EXTRA_DOCUMENT_METADATA = "analysis_result_document_metadata"

        fun createIntent(
            context: Context,
            documentName: String,
            documentMetadata: String
        ): Intent = Intent(context, AnalysisResultActivity::class.java)
            .putExtra(EXTRA_DOCUMENT_NAME, documentName)
            .putExtra(EXTRA_DOCUMENT_METADATA, documentMetadata)
    }
}
