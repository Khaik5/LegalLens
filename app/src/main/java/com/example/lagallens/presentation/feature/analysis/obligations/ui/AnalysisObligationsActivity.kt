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

class AnalysisObligationsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAnalysisObligationsBinding
    private val viewModel by viewModels<AnalysisObligationsViewModel>()
    private val obligationsAdapter = AnalysisObligationsAdapter()

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
        binding.analysisObligationsItems.apply {
            layoutManager = LinearLayoutManager(this@AnalysisObligationsActivity)
            adapter = obligationsAdapter
        }
        observeViewModel()
        viewModel.onEvent(
            AnalysisObligationsUiEvent.ScreenOpened(
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

    private fun render(state: AnalysisObligationsUiState) = with(binding) {
        analysisObligationsDocumentName.text = state.documentName
        analysisObligationsDocumentMetadata.text = state.documentMetadata
        obligationsAdapter.submitList(state.items)
    }

    private fun handleEffect(effect: AnalysisObligationsUiEffect) {
        when (effect) {
            AnalysisObligationsUiEffect.NavigateBack -> finish()
        }
    }

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
