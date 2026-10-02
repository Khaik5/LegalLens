package com.example.lagallens.presentation.feature.ocr.saved.ui

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
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityOcrSavedBinding
import com.example.lagallens.presentation.feature.analysis.start.ui.AnalysisStartActivity
import com.example.lagallens.presentation.feature.ocr.saved.contract.OcrSavedUiEffect
import com.example.lagallens.presentation.feature.ocr.saved.contract.OcrSavedUiEvent
import com.example.lagallens.presentation.feature.ocr.saved.contract.OcrSavedUiState
import com.example.lagallens.presentation.feature.ocr.saved.viewmodel.OcrSavedViewModel
import kotlinx.coroutines.launch

class OcrSavedActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOcrSavedBinding
    private val viewModel by viewModels<OcrSavedViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityOcrSavedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.ocrSavedRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        bindEvents()
        observeViewModel()
        viewModel.onEvent(
            OcrSavedUiEvent.ScreenOpened(
                documentName = intent.getStringExtra(EXTRA_DOCUMENT_NAME)
                    .orEmpty()
                    .ifBlank { getString(R.string.ocr_saved_document_name) },
                documentVersion = intent.getStringExtra(EXTRA_DOCUMENT_VERSION)
                    .orEmpty()
                    .ifBlank { getString(R.string.ocr_saved_document_version) },
                documentStatus = intent.getStringExtra(EXTRA_DOCUMENT_STATUS)
                    .orEmpty()
                    .ifBlank { getString(R.string.ocr_saved_document_status) }
            )
        )
    }

    private fun bindEvents() = with(binding) {
        ocrSavedReturn.setOnClickListener {
            viewModel.onEvent(OcrSavedUiEvent.ReturnToContractClicked)
        }
        ocrSavedAnalyze.setOnClickListener {
            viewModel.onEvent(OcrSavedUiEvent.AnalyzeClicked)
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

    private fun render(state: OcrSavedUiState) = with(binding) {
        ocrSavedDocumentName.text = state.documentName
        ocrSavedDocumentVersion.text = state.documentVersion
        ocrSavedDocumentStatus.text = state.documentStatus
    }

    private fun handleEffect(effect: OcrSavedUiEffect) {
        when (effect) {
            OcrSavedUiEffect.NavigateBack -> finish()
            is OcrSavedUiEffect.NavigateToAnalysis -> {
                startActivity(AnalysisStartActivity.createIntent(this, effect.documentName))
            }
        }
    }

    companion object {
        private const val EXTRA_DOCUMENT_NAME = "ocr_saved_document_name"
        private const val EXTRA_DOCUMENT_VERSION = "ocr_saved_document_version"
        private const val EXTRA_DOCUMENT_STATUS = "ocr_saved_document_status"

        fun createIntent(
            context: Context,
            documentName: String,
            documentVersion: String,
            documentStatus: String
        ): Intent = Intent(context, OcrSavedActivity::class.java)
            .putExtra(EXTRA_DOCUMENT_NAME, documentName)
            .putExtra(EXTRA_DOCUMENT_VERSION, documentVersion)
            .putExtra(EXTRA_DOCUMENT_STATUS, documentStatus)
    }
}
