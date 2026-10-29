package com.example.lagallens.presentation.feature.ocr.saved.ui

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
import com.example.lagallens.presentation.feature.main.ui.MainActivity
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityOcrSavedBinding
import com.example.lagallens.presentation.common.utils.bindPressFeedback
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
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = viewModel.onEvent(OcrSavedUiEvent.ReturnToContractClicked)
        })
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
                    .ifBlank { getString(R.string.ocr_saved_document_status) },
                documentId = intent.getStringExtra(EXTRA_DOCUMENT_ID).orEmpty(),
                contractId = intent.getStringExtra(EXTRA_CONTRACT_ID).orEmpty()
            )
        )
    }

    private fun bindEvents() = with(binding) {
        listOf(ocrSavedReturn, ocrSavedAnalyze).forEach { it.bindPressFeedback() }
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
            OcrSavedUiEffect.NavigateBack -> {
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
                finish()
            }
            is OcrSavedUiEffect.NavigateToAnalysis -> {
                startActivity(AnalysisStartActivity.createIntent(this, effect.documentName).putExtra("analysis_return_home", true))
            }
        }
    }

    companion object {
        private const val EXTRA_DOCUMENT_NAME = "ocr_saved_document_name"
        private const val EXTRA_DOCUMENT_VERSION = "ocr_saved_document_version"
        private const val EXTRA_DOCUMENT_STATUS = "ocr_saved_document_status"
        private const val EXTRA_DOCUMENT_URI = "ocr_saved_document_uri"
        private const val EXTRA_DOCUMENT_ID = "ocr_saved_document_id"
        private const val EXTRA_CONTRACT_ID = "ocr_saved_contract_id"

        fun createIntent(
            context: Context,
            documentName: String,
            documentVersion: String,
            documentStatus: String,
            documentUri: String? = null,
            documentId: String = "",
            contractId: String = ""
        ): Intent = Intent(context, OcrSavedActivity::class.java)
            .putExtra(EXTRA_DOCUMENT_NAME, documentName)
            .putExtra(EXTRA_DOCUMENT_VERSION, documentVersion)
            .putExtra(EXTRA_DOCUMENT_STATUS, documentStatus)
            .putExtra(EXTRA_DOCUMENT_URI, documentUri)
            .putExtra(EXTRA_DOCUMENT_ID, documentId)
            .putExtra(EXTRA_CONTRACT_ID, contractId)
    }
}
