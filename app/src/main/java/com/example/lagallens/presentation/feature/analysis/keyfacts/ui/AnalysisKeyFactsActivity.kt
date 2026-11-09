package com.example.lagallens.presentation.feature.analysis.keyfacts.ui

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
import com.example.lagallens.databinding.ActivityAnalysisKeyFactsBinding
import com.example.lagallens.presentation.feature.analysis.keyfacts.adapter.AnalysisKeyFactsAdapter
import com.example.lagallens.presentation.feature.analysis.keyfacts.contract.AnalysisClauseSource
import com.example.lagallens.presentation.feature.analysis.keyfacts.dialog.AnalysisClauseSourceBottomSheet
import com.example.lagallens.presentation.feature.analysis.keyfacts.contract.AnalysisKeyFactsUiEffect
import com.example.lagallens.presentation.feature.analysis.keyfacts.contract.AnalysisKeyFactsUiEvent
import com.example.lagallens.presentation.feature.analysis.keyfacts.contract.AnalysisKeyFactsUiState
import com.example.lagallens.presentation.feature.analysis.keyfacts.viewmodel.AnalysisKeyFactsViewModel
import kotlinx.coroutines.launch

class AnalysisKeyFactsActivity : com.example.lagallens.presentation.feature.analysis.AnalysisScopedActivity(), com.example.lagallens.presentation.feature.analysis.keyfacts.contract.AnalysisClauseSourceOwner {
    override val requiresUnlock = false
    override val observesUnlock = true
    private lateinit var binding: ActivityAnalysisKeyFactsBinding
    private val viewModel by viewModels<AnalysisKeyFactsViewModel>()
    private val lockedAdapter = com.example.lagallens.presentation.feature.analysis.adapter.AnalysisLockedPreviewAdapter { openUnlockConfirmation(viewModel.uiState.value.documentName) }
    private val keyFactsAdapter = AnalysisKeyFactsAdapter { source ->
        viewModel.onEvent(AnalysisKeyFactsUiEvent.SourceClicked(source))
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
        binding = ActivityAnalysisKeyFactsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.analysisKeyFactsRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.analysisKeyFactsBack.setOnClickListener {
            viewModel.onEvent(AnalysisKeyFactsUiEvent.BackClicked)
        }
        binding.analysisKeyFactsItems.apply {
            layoutManager = LinearLayoutManager(this@AnalysisKeyFactsActivity)
            adapter = androidx.recyclerview.widget.ConcatAdapter(keyFactsAdapter, lockedAdapter)
            isNestedScrollingEnabled = false
        }
        observeViewModel()
        viewModel.onEvent(
            AnalysisKeyFactsUiEvent.ScreenOpened(
                documentName = intent.getStringExtra(EXTRA_DOCUMENT_NAME)
                    .orEmpty(),
                documentMetadata = intent.getStringExtra(EXTRA_DOCUMENT_METADATA)
                    .orEmpty()
            )
        )
    }

    override fun onAnalysisInvalidated() { viewModel.clearResult() }

    override fun onAnalysisValidated(identity: com.example.lagallens.domain.model.analysis.AnalysisIdentity, detail: com.example.lagallens.domain.model.analysis.AnalysisDetail) {
        viewModel.setResult(identity, detail, validatedUnlockStatus?.isUnlocked == true)
        showContentStatus(viewModel.uiState.value.status)
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

    private fun render(state: AnalysisKeyFactsUiState) = with(binding) {
        analysisContentEmpty.visibility = if (state.status == com.example.lagallens.presentation.feature.analysis.SnapshotContentStatus.EMPTY) android.view.View.VISIBLE else android.view.View.GONE
        analysisKeyFactsDocumentName.text = state.documentName
        analysisKeyFactsDocumentMetadata.text = getString(R.string.analysis_snapshot_demo_metadata, state.documentMetadata)
        keyFactsAdapter.submitList(state.facts)
        lockedAdapter.update(state.lockedCount, validatedUnlockStatus?.cost)
    }

    private fun handleEffect(effect: AnalysisKeyFactsUiEffect) {
        when (effect) {
            AnalysisKeyFactsUiEffect.NavigateBack -> finish()
            is AnalysisKeyFactsUiEffect.ShowClauseSource -> showClauseSource(effect.source)
        }
    }

    private fun showClauseSource(source: AnalysisClauseSource) {
        if (supportFragmentManager.findFragmentByTag(AnalysisClauseSourceBottomSheet.TAG) == null) {
            AnalysisClauseSourceBottomSheet.newInstance(source).show(
                supportFragmentManager,
                AnalysisClauseSourceBottomSheet.TAG
            )
        }
    }

    override fun isVerifiedClause(source: AnalysisClauseSource): Boolean =
        hasValidatedAnalysis() && viewModel.uiState.value.facts.any { fact -> fact.source == source }

    override fun onDestroy() {
        binding.analysisKeyFactsItems.adapter = null
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_DOCUMENT_NAME = "analysis_key_facts_document_name"
        private const val EXTRA_DOCUMENT_METADATA = "analysis_key_facts_document_metadata"

        fun createIntent(
            context: Context,
            documentName: String,
            documentMetadata: String
        ): Intent = Intent(context, AnalysisKeyFactsActivity::class.java)
            .putExtra(EXTRA_DOCUMENT_NAME, documentName)
            .putExtra(EXTRA_DOCUMENT_METADATA, documentMetadata)
    }
}
