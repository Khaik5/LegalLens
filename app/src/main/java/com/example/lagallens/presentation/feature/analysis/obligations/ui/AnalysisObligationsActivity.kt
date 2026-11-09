package com.example.lagallens.presentation.feature.analysis.obligations.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
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
import com.example.lagallens.databinding.ActivityAnalysisObligationsBinding
import com.example.lagallens.presentation.feature.analysis.obligations.adapter.AnalysisObligationsAdapter
import com.example.lagallens.presentation.feature.analysis.obligations.contract.AnalysisObligationsUiEffect
import com.example.lagallens.presentation.feature.analysis.obligations.contract.AnalysisObligationsUiEvent
import com.example.lagallens.presentation.feature.analysis.obligations.contract.AnalysisObligationsUiState
import com.example.lagallens.presentation.feature.analysis.obligations.viewmodel.AnalysisObligationsViewModel
import kotlinx.coroutines.launch

class AnalysisObligationsActivity : com.example.lagallens.presentation.feature.analysis.AnalysisScopedActivity(), com.example.lagallens.presentation.feature.analysis.keyfacts.contract.AnalysisClauseSourceOwner {
    override val requiresUnlock = false
    override val observesUnlock = true
    private lateinit var binding: ActivityAnalysisObligationsBinding
    private val viewModel by viewModels<AnalysisObligationsViewModel>()
    private val obligationsAdapter = AnalysisObligationsAdapter { id -> viewModel.onEvent(AnalysisObligationsUiEvent.SourceClicked(id)) }
    private val lockedAdapter = com.example.lagallens.presentation.feature.analysis.adapter.AnalysisLockedPreviewAdapter { openUnlockConfirmation(viewModel.uiState.value.documentName) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityAnalysisObligationsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.analysisObligationsRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                viewModel.onEvent(AnalysisObligationsUiEvent.BackClicked)
            }
        })
        binding.analysisObligationsBack.setOnClickListener { viewModel.onEvent(AnalysisObligationsUiEvent.BackClicked) }
        binding.analysisObligationsTabRights.setOnClickListener { viewModel.onEvent(AnalysisObligationsUiEvent.TabSelected(com.example.lagallens.domain.model.analysis.AnalysisObligationTab.RIGHTS)) }
        binding.analysisObligationsTabDuties.setOnClickListener { viewModel.onEvent(AnalysisObligationsUiEvent.TabSelected(com.example.lagallens.domain.model.analysis.AnalysisObligationTab.DUTIES)) }
        binding.analysisObligationsItems.apply {
            layoutManager = LinearLayoutManager(this@AnalysisObligationsActivity)
            adapter = androidx.recyclerview.widget.ConcatAdapter(obligationsAdapter, lockedAdapter)
        }
        observeViewModel()
        viewModel.onEvent(
            AnalysisObligationsUiEvent.ScreenOpened(
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

    private fun render(state: AnalysisObligationsUiState) = with(binding) {
        analysisContentEmpty.visibility = if (state.status == com.example.lagallens.presentation.feature.analysis.SnapshotContentStatus.EMPTY) android.view.View.VISIBLE else android.view.View.GONE
        analysisObligationsDocumentName.text = state.documentName
        analysisObligationsDocumentMetadata.text = getString(R.string.analysis_snapshot_demo_metadata, state.documentMetadata)
        obligationsAdapter.submitList(state.items)
        lockedAdapter.update(state.lockedCount, validatedUnlockStatus?.cost)
        val rights = state.selectedTab == com.example.lagallens.domain.model.analysis.AnalysisObligationTab.RIGHTS
        analysisObligationsTabRights.setBackgroundResource(if (rights) R.drawable.bg_analysis_obligations_selected_tab else android.R.color.transparent)
        analysisObligationsTabDuties.setBackgroundResource(if (!rights) R.drawable.bg_analysis_obligations_selected_tab else android.R.color.transparent)
        analysisObligationsTabRights.setTextColor(getColor(if (rights) R.color.legal_lens_primary else R.color.legal_lens_secondary_text))
        analysisObligationsTabDuties.setTextColor(getColor(if (!rights) R.color.legal_lens_primary else R.color.legal_lens_secondary_text))
        analysisObligationsTabRights.isSelected = rights
        analysisObligationsTabDuties.isSelected = !rights
    }

    private fun handleEffect(effect: AnalysisObligationsUiEffect) {
        when (effect) {
            AnalysisObligationsUiEffect.NavigateBack -> finish()
            AnalysisObligationsUiEffect.SourceUnavailable -> android.widget.Toast.makeText(this, R.string.analysis_obligation_source_unavailable, android.widget.Toast.LENGTH_SHORT).show()
            is AnalysisObligationsUiEffect.ShowSource -> {
                val tag = com.example.lagallens.presentation.feature.analysis.keyfacts.dialog.AnalysisClauseSourceBottomSheet.TAG
                if (supportFragmentManager.findFragmentByTag(tag) == null) com.example.lagallens.presentation.feature.analysis.keyfacts.dialog.AnalysisClauseSourceBottomSheet.newInstance(effect.source).show(supportFragmentManager, tag)
            }
        }
    }

    override fun isVerifiedClause(source: com.example.lagallens.presentation.feature.analysis.keyfacts.contract.AnalysisClauseSource): Boolean =
        hasValidatedAnalysis() && viewModel.uiState.value.items.filterIsInstance<com.example.lagallens.presentation.feature.analysis.obligations.contract.AnalysisObligationsListItem.Obligation>().any { it.obligation.source == source }

    override fun onDestroy() {
        binding.analysisObligationsItems.adapter = null
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_DOCUMENT_NAME = "analysis_obligations_document_name"
        private const val EXTRA_DOCUMENT_METADATA = "analysis_obligations_document_metadata"

        fun createIntent(
            context: Context,
            documentName: String,
            documentMetadata: String
        ): Intent = Intent(context, AnalysisObligationsActivity::class.java)
            .putExtra(EXTRA_DOCUMENT_NAME, documentName)
            .putExtra(EXTRA_DOCUMENT_METADATA, documentMetadata)
    }
}
