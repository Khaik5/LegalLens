package com.example.lagallens.presentation.feature.ocr.result.ui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityOcrResultBinding
import com.example.lagallens.presentation.feature.ocr.result.contract.OcrResultUiEffect
import com.example.lagallens.presentation.feature.ocr.result.contract.OcrResultUiEvent
import com.example.lagallens.presentation.feature.ocr.result.contract.OcrResultUiState
import com.example.lagallens.presentation.feature.ocr.result.viewmodel.OcrResultViewModel
import com.example.lagallens.presentation.feature.ocr.editor.ui.OcrEditorActivity
import com.example.lagallens.presentation.feature.ocr.saved.ui.OcrSavedActivity
import com.example.lagallens.presentation.feature.ocr.ui.OcrReviewActivity
import kotlinx.coroutines.launch

class OcrResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOcrResultBinding
    private val viewModel by viewModels<OcrResultViewModel> {
        OcrResultViewModel.Factory(
            (application as LegalLensApplication).getOcrResultUseCase
        )
    }
    private val ocrReviewLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            if (result.data?.getBooleanExtra(OcrReviewActivity.EXTRA_REVIEW_CONFIRMED, false) == true) {
                viewModel.onEvent(OcrResultUiEvent.ReviewConfirmed)
            }
        }
    }
    private val ocrEditorLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.getStringExtra(OcrEditorActivity.EXTRA_EDITED_TEXT)?.let {
                viewModel.onEvent(OcrResultUiEvent.DocumentUpdated(it))
            }
        }
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
        binding = ActivityOcrResultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel.onEvent(
            OcrResultUiEvent.ScreenOpened(
                documentName = intent.getStringExtra(EXTRA_DOCUMENT_NAME).orEmpty(),
                documentDetails = intent.getStringExtra(EXTRA_DOCUMENT_DETAILS).orEmpty()
            )
        )
        ViewCompat.setOnApplyWindowInsetsListener(binding.ocrResultRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
        bindEvents()
        observeViewModel()
    }

    private fun bindEvents() = with(binding) {
        ocrResultBack.setOnClickListener { viewModel.onEvent(OcrResultUiEvent.BackClicked) }
        ocrResultEdit.setOnClickListener { viewModel.onEvent(OcrResultUiEvent.EditClicked) }
        ocrResultReview.setOnClickListener { viewModel.onEvent(OcrResultUiEvent.ReviewClicked) }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.uiEffect.collect(::handleEffect) }
            }
        }
    }

    private fun render(state: OcrResultUiState) = with(binding) {
        ocrResultConfidenceLabel.text = state.confidenceLabel
        ocrResultConfidencePercentage.text = state.confidencePercentage
        ocrResultDocument.text = state.documentText
    }

    private fun handleEffect(effect: OcrResultUiEffect) {
        when (effect) {
            OcrResultUiEffect.NavigateBack -> finish()
            is OcrResultUiEffect.NavigateToEdit -> {
                ocrEditorLauncher.launch(OcrEditorActivity.createIntent(this, effect.documentText))
            }
            OcrResultUiEffect.NavigateToReview -> {
                ocrReviewLauncher.launch(OcrReviewActivity.createIntent(this))
            }
            is OcrResultUiEffect.NavigateToSaved -> {
                startActivity(
                    OcrSavedActivity.createIntent(
                        context = this,
                        documentName = effect.documentName,
                        documentVersion = "",
                        documentStatus = ""
                    )
                )
                finish()
            }
        }
    }

    companion object {
        private const val EXTRA_DOCUMENT_NAME = "ocr_result_document_name"
        private const val EXTRA_DOCUMENT_DETAILS = "ocr_result_document_details"

        fun createIntent(
            context: android.content.Context,
            documentName: String,
            documentDetails: String
        ): android.content.Intent = android.content.Intent(context, OcrResultActivity::class.java)
            .putExtra(EXTRA_DOCUMENT_NAME, documentName)
            .putExtra(EXTRA_DOCUMENT_DETAILS, documentDetails)
    }
}
