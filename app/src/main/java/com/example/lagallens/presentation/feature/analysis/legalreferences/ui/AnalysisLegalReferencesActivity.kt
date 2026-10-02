package com.example.lagallens.presentation.feature.analysis.legalreferences.ui

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
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityAnalysisLegalReferencesBinding
import com.example.lagallens.presentation.feature.analysis.legalreferences.adapter.AnalysisLegalReferencesAdapter
import com.example.lagallens.presentation.feature.analysis.legalreferences.contract.AnalysisLegalReferencesUiEffect
import com.example.lagallens.presentation.feature.analysis.legalreferences.contract.AnalysisLegalReferencesUiEvent
import com.example.lagallens.presentation.feature.analysis.legalreferences.contract.AnalysisLegalReferencesUiState
import com.example.lagallens.presentation.feature.analysis.legalreferences.detail.ui.AnalysisLegalReferenceDetailActivity
import com.example.lagallens.presentation.feature.analysis.legalreferences.viewmodel.AnalysisLegalReferencesViewModel
import kotlinx.coroutines.launch

class AnalysisLegalReferencesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAnalysisLegalReferencesBinding
    private val viewModel by viewModels<AnalysisLegalReferencesViewModel>()
    private val referencesAdapter = AnalysisLegalReferencesAdapter { referenceId ->
        viewModel.onEvent(AnalysisLegalReferencesUiEvent.ReferenceClicked(referenceId))
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
        binding = ActivityAnalysisLegalReferencesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.analysisLegalReferencesRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        bindViews()
        observeViewModel()
        viewModel.onEvent(
            AnalysisLegalReferencesUiEvent.ScreenOpened(
                documentName = intent.getStringExtra(EXTRA_DOCUMENT_NAME).orEmpty(),
                documentMetadata = intent.getStringExtra(EXTRA_DOCUMENT_METADATA).orEmpty()
            )
        )
    }

    private fun bindViews() = with(binding) {
        analysisLegalReferencesBack.setOnClickListener {
            viewModel.onEvent(AnalysisLegalReferencesUiEvent.BackClicked)
        }
        analysisLegalReferencesSearch.doAfterTextChanged { editable ->
            viewModel.onEvent(
                AnalysisLegalReferencesUiEvent.QueryChanged(editable?.toString().orEmpty())
            )
        }
        analysisLegalReferencesList.apply {
            layoutManager = LinearLayoutManager(this@AnalysisLegalReferencesActivity)
            adapter = referencesAdapter
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

    private fun render(state: AnalysisLegalReferencesUiState) {
        referencesAdapter.submitList(state.references)
    }

    private fun handleEffect(effect: AnalysisLegalReferencesUiEffect) {
        when (effect) {
            AnalysisLegalReferencesUiEffect.NavigateBack -> finish()
            AnalysisLegalReferencesUiEffect.ShowDetailUnavailable -> Toast.makeText(
                this,
                R.string.analysis_legal_references_detail_unavailable,
                Toast.LENGTH_SHORT
            ).show()

            is AnalysisLegalReferencesUiEffect.NavigateToDetail -> startActivity(
                AnalysisLegalReferenceDetailActivity.createIntent(
                    context = this,
                    referenceId = effect.referenceId,
                    documentName = effect.documentName,
                    documentMetadata = effect.documentMetadata
                )
            )
        }
    }

    companion object {
        private const val EXTRA_DOCUMENT_NAME = "analysis_legal_references_document_name"
        private const val EXTRA_DOCUMENT_METADATA = "analysis_legal_references_document_metadata"

        fun createIntent(
            context: Context,
            documentName: String,
            documentMetadata: String
        ): Intent = Intent(context, AnalysisLegalReferencesActivity::class.java)
            .putExtra(EXTRA_DOCUMENT_NAME, documentName)
            .putExtra(EXTRA_DOCUMENT_METADATA, documentMetadata)
    }
}
