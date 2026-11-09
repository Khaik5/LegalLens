package com.example.lagallens.presentation.feature.ocr.failure.ui

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
import com.example.lagallens.databinding.ActivityOcrFailureBinding
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import com.example.lagallens.presentation.feature.camera.ocr.ui.OcrProcessingActivity
import com.example.lagallens.presentation.feature.ocr.failure.contract.OcrFailureUiEffect
import com.example.lagallens.presentation.feature.ocr.failure.contract.OcrFailureUiEvent
import com.example.lagallens.presentation.feature.ocr.failure.viewmodel.OcrFailureViewModel
import kotlinx.coroutines.launch

class OcrFailureActivity : com.example.lagallens.presentation.feature.ocr.OcrSessionActivity() {
    private lateinit var binding: ActivityOcrFailureBinding
    private val viewModel by viewModels<OcrFailureViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isFinishing || !isOcrSessionValid() || intent.getStringExtra(EXTRA_DOCUMENT_ID).isNullOrBlank()) { finish(); return }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityOcrFailureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.ocrFailureRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
        bindEvents()
        observeViewModel()
        viewModel.onEvent(
            OcrFailureUiEvent.ScreenOpened(
                documentName = intent.getStringExtra(EXTRA_DOCUMENT_NAME).orEmpty(),
                documentDetails = intent.getStringExtra(EXTRA_DOCUMENT_DETAILS).orEmpty()
            )
        )
    }

    private fun bindEvents() = with(binding) {
        listOf(ocrFailureBack, ocrFailureReturn, ocrFailureRetry).forEach { it.bindPressFeedback() }
        ocrFailureBack.setOnClickListener { viewModel.onEvent(OcrFailureUiEvent.BackClicked) }
        ocrFailureReturn.setOnClickListener { viewModel.onEvent(OcrFailureUiEvent.BackClicked) }
        ocrFailureRetry.setOnClickListener { viewModel.onEvent(OcrFailureUiEvent.RetryClicked) }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiEffect.collect(::handleEffect) }
            }
        }
    }

    private fun handleEffect(effect: OcrFailureUiEffect) {
        if (!isOcrSessionValid()) return
        when (effect) {
            OcrFailureUiEffect.NavigateBack -> finish()
            is OcrFailureUiEffect.RetryOcr -> {
                startActivity(
                    OcrProcessingActivity.createIntent(
                        context = this,
                        documentName = effect.documentName,
                        documentDetails = effect.documentDetails,
                        documentId = intent.getStringExtra(EXTRA_DOCUMENT_ID).orEmpty(),
                        documentUri = intent.getStringExtra(EXTRA_DOCUMENT_URI),
                        pagePaths = intent.getStringArrayListExtra(EXTRA_PAGE_PATHS).orEmpty(),
                        mimeType = intent.getStringExtra(EXTRA_MIME_TYPE).orEmpty()
                    )
                )
                finish()
            }
        }
    }

    companion object {
        private const val EXTRA_DOCUMENT_NAME = "ocr_failure_document_name"
        private const val EXTRA_DOCUMENT_DETAILS = "ocr_failure_document_details"
        private const val EXTRA_DOCUMENT_ID = "ocr_failure_document_id"
        private const val EXTRA_DOCUMENT_URI = "ocr_failure_document_uri"
        private const val EXTRA_PAGE_PATHS = "ocr_failure_page_paths"
        private const val EXTRA_MIME_TYPE = "ocr_failure_mime_type"

        fun createIntent(
            context: Context,
            documentName: String,
            documentDetails: String,
            documentId: String,
            documentUri: String?,
            pagePaths: List<String> = emptyList(),
            mimeType: String = ""
        ): Intent = com.example.lagallens.presentation.feature.ocr.OcrSession.stamp(context, Intent(context, OcrFailureActivity::class.java)
            .putExtra(EXTRA_DOCUMENT_NAME, documentName)
            .putExtra(EXTRA_DOCUMENT_DETAILS, documentDetails)
            .putExtra(EXTRA_DOCUMENT_ID, documentId)
            .putExtra(EXTRA_DOCUMENT_URI, documentUri)
            .putExtra(EXTRA_MIME_TYPE, mimeType)
            .putStringArrayListExtra(EXTRA_PAGE_PATHS, ArrayList(pagePaths)))
    }
}
