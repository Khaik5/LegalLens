package com.example.lagallens.presentation.feature.ocr.result.ui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityOcrResultBinding
import com.example.lagallens.presentation.common.utils.animateContentEntrance
import com.example.lagallens.presentation.common.utils.bindPressFeedback
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
            (application as LegalLensApplication).getOcrResultUseCase,
            (application as LegalLensApplication).documentRepository,
            (application as LegalLensApplication).contractRepository
        )
    }
    private val ocrReviewLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            if (result.data?.getBooleanExtra(OcrReviewActivity.EXTRA_REVIEW_CONFIRMED, false) == true) {
                viewModel.onEvent(OcrResultUiEvent.ReviewConfirmed(
                    result.data?.getStringExtra(OcrReviewActivity.EXTRA_DOCUMENT_TEXT),
                    result.data?.getStringExtra(OcrReviewActivity.EXTRA_DOCUMENT_ID),
                    result.data?.getStringExtra(OcrReviewActivity.EXTRA_CONTRACT_ID)
                ))
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
        if (savedInstanceState == null) binding.ocrResultDocument.doOnPreDraw {
            it.animateContentEntrance(0f)
        }
        viewModel.onEvent(
            OcrResultUiEvent.ScreenOpened(
                documentName = intent.getStringExtra(EXTRA_DOCUMENT_NAME).orEmpty(),
                documentDetails = intent.getStringExtra(EXTRA_DOCUMENT_DETAILS).orEmpty(),
                documentId = intent.getStringExtra(EXTRA_DOCUMENT_ID).orEmpty(),
                contractId = intent.getStringExtra(EXTRA_CONTRACT_ID),
                initialContent = intent.getStringExtra(EXTRA_INITIAL_CONTENT).orEmpty(),
                documentUri = intent.getStringExtra(EXTRA_DOCUMENT_URI)
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
        listOf(ocrResultBack, ocrResultEdit, ocrResultReview).forEach { it.bindPressFeedback() }
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
        if (state.errorMessageRes != null) {
            ocrResultConfidenceLabel.text = ""
            ocrResultConfidencePercentage.text = ""
            ocrResultDocument.setText(state.errorMessageRes)
        } else {
            ocrResultConfidenceLabel.text = state.confidenceLabel
            ocrResultConfidencePercentage.text = state.confidencePercentage
            ocrResultDocument.text = state.documentText
        }
    }

    private fun handleEffect(effect: OcrResultUiEffect) {
        when (effect) {
            OcrResultUiEffect.NavigateBack -> finish()
            is OcrResultUiEffect.NavigateToEdit -> {
                ocrEditorLauncher.launch(OcrEditorActivity.createIntent(this, effect.documentText))
            }
            OcrResultUiEffect.NavigateToReview -> {
                val state = viewModel.uiState.value
                ocrReviewLauncher.launch(OcrReviewActivity.createIntent(this, state.documentId, state.contractId, state.documentText))
            }
            is OcrResultUiEffect.NavigateToSaved -> {
                startActivity(
                    OcrSavedActivity.createIntent(
                        context = this,
                        documentName = effect.documentName,
                        documentVersion = "",
                        documentStatus = "",
                        documentUri = intent.getStringExtra(EXTRA_DOCUMENT_URI),
                        documentId = effect.documentId,
                        contractId = effect.contractId
                    )
                )
                finish()
            }
        }
    }

    companion object {
        private const val EXTRA_DOCUMENT_NAME = "ocr_result_document_name"
        private const val EXTRA_DOCUMENT_DETAILS = "ocr_result_document_details"
        private const val EXTRA_DOCUMENT_URI = "ocr_result_document_uri"
        private const val EXTRA_DOCUMENT_ID = "ocr_result_document_id"
        private const val EXTRA_CONTRACT_ID = "ocr_result_contract_id"
        private const val EXTRA_INITIAL_CONTENT = "ocr_result_initial_content"

        fun createIntent(
            context: android.content.Context,
            documentName: String,
            documentDetails: String,
            documentUri: String? = null,
            documentId: String = java.util.UUID.randomUUID().toString(),
            contractId: String? = null,
            initialContent: String = ""
        ): android.content.Intent = android.content.Intent(context, OcrResultActivity::class.java)
            .putExtra(EXTRA_DOCUMENT_NAME, documentName)
            .putExtra(EXTRA_DOCUMENT_DETAILS, documentDetails)
            .putExtra(EXTRA_DOCUMENT_URI, documentUri)
            .putExtra(EXTRA_DOCUMENT_ID, documentId)
            .putExtra(EXTRA_CONTRACT_ID, contractId)
            .putExtra(EXTRA_INITIAL_CONTENT, initialContent)
    }
}
