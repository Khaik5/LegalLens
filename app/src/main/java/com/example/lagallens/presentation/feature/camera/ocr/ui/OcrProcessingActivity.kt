package com.example.lagallens.presentation.feature.camera.ocr.ui

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
import com.example.lagallens.databinding.ActivityOcrProcessingBinding
import com.example.lagallens.domain.model.camera.ScanPage
import com.example.lagallens.presentation.feature.camera.ocr.contract.OcrProcessingUiEffect
import com.example.lagallens.presentation.feature.camera.ocr.contract.OcrProcessingUiEvent
import com.example.lagallens.presentation.feature.camera.ocr.contract.OcrProcessingUiState
import com.example.lagallens.presentation.feature.ocr.failure.ui.OcrFailureActivity
import com.example.lagallens.presentation.feature.ocr.result.ui.OcrResultActivity
import com.example.lagallens.presentation.feature.camera.ocr.viewmodel.OcrProcessingViewModel
import kotlinx.coroutines.launch

class OcrProcessingActivity : AppCompatActivity() {
    private val binding by lazy { ActivityOcrProcessingBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<OcrProcessingViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContentView(binding.root)
        applyWindowInsets()
        initializeDocumentMetadata()
        bindView()
        collectState()
        viewModel.onEvent(
            OcrProcessingUiEvent.ScreenOpened(
                shouldSimulateFailure = intent.getBooleanExtra(EXTRA_SHOULD_SIMULATE_FAILURE, false)
            )
        )
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.ocrProcessingRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initializeDocumentMetadata() {
        val pageCount = intent.getStringArrayListExtra(EXTRA_PAGE_PATHS).orEmpty().size
        val documentName = intent.getStringExtra(EXTRA_DOCUMENT_NAME)
            .orEmpty()
            .ifBlank { getString(R.string.ocr_processing_scanned_document_name) }
        val documentDetails = intent.getStringExtra(EXTRA_DOCUMENT_DETAILS)
            .orEmpty()
            .ifBlank { getString(R.string.ocr_processing_scanned_document_details, pageCount) }
        viewModel.setDocumentMetadata(documentName, documentDetails)
    }

    private fun bindView() {
        binding.ocrProcessingBack.setOnClickListener { finish() }
    }

    private fun collectState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.uiEffect.collect(::handleEffect) }
            }
        }
    }

    private fun render(state: OcrProcessingUiState) {
        binding.ocrProcessingDocumentName.text = state.documentName
        binding.ocrProcessingDocumentDetails.text = state.documentDetails
    }

    private fun handleEffect(effect: OcrProcessingUiEffect) {
        when (effect) {
            is OcrProcessingUiEffect.NavigateToOcrResult -> {
                startActivity(
                    OcrResultActivity.createIntent(
                        context = this,
                        documentName = effect.documentName,
                        documentDetails = effect.documentDetails
                    )
                )
                finish()
            }
            is OcrProcessingUiEffect.NavigateToOcrFailure -> {
                startActivity(
                    OcrFailureActivity.createIntent(
                        context = this,
                        documentName = effect.documentName,
                        documentDetails = effect.documentDetails
                    )
                )
                finish()
            }
        }
    }

    companion object {
        private const val EXTRA_PAGE_PATHS = "ocr_page_paths"
        private const val EXTRA_DOCUMENT_NAME = "ocr_document_name"
        private const val EXTRA_DOCUMENT_DETAILS = "ocr_document_details"
        private const val EXTRA_SHOULD_SIMULATE_FAILURE = "ocr_should_simulate_failure"

        fun createIntent(
            context: Context,
            pages: List<ScanPage>,
            shouldSimulateFailure: Boolean = false
        ): Intent = Intent(
            context,
            OcrProcessingActivity::class.java
        ).putStringArrayListExtra(EXTRA_PAGE_PATHS, ArrayList(pages.map(ScanPage::path)))
            .putExtra(EXTRA_SHOULD_SIMULATE_FAILURE, shouldSimulateFailure)

        fun createIntent(
            context: Context,
            documentName: String,
            documentDetails: String,
            shouldSimulateFailure: Boolean = false
        ): Intent = Intent(
            context,
            OcrProcessingActivity::class.java
        ).putExtra(EXTRA_DOCUMENT_NAME, documentName)
            .putExtra(EXTRA_DOCUMENT_DETAILS, documentDetails)
            .putExtra(EXTRA_SHOULD_SIMULATE_FAILURE, shouldSimulateFailure)
    }
}
