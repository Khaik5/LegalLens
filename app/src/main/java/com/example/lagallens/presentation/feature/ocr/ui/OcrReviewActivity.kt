package com.example.lagallens.presentation.feature.ocr.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.Spannable
import android.text.style.StyleSpan
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityOcrReviewBinding
import com.example.lagallens.presentation.feature.ocr.contract.OcrReviewEffect
import com.example.lagallens.presentation.feature.ocr.contract.OcrReviewEvent
import com.example.lagallens.presentation.feature.ocr.contract.OcrReviewUiState
import com.example.lagallens.presentation.feature.ocr.contract.OcrReviewWarning
import com.example.lagallens.presentation.feature.ocr.viewmodel.OcrReviewViewModel
import kotlinx.coroutines.launch

class OcrReviewActivity : com.example.lagallens.presentation.feature.ocr.OcrSessionActivity() {
    private lateinit var binding: ActivityOcrReviewBinding
    private val viewModel by viewModels<OcrReviewViewModel>()

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
        binding = ActivityOcrReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.ocrReviewRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                maxOf(systemBars.bottom, ime.bottom)
            )
            insets
        }
        ViewCompat.requestApplyInsets(binding.ocrReviewRoot)
        viewModel.onEvent(OcrReviewEvent.ScreenOpened(
            intent.getStringExtra(EXTRA_DOCUMENT_ID).orEmpty(),
            intent.getStringExtra(EXTRA_CONTRACT_ID),
            intent.getStringExtra(EXTRA_DOCUMENT_TEXT).orEmpty(),
            warningsFrom(intent)
        ))
        bindEvents()
        observeViewModel()
    }

    private fun bindEvents() = with(binding) {
        ocrReviewBack.setOnClickListener { viewModel.onEvent(OcrReviewEvent.BackClicked) }
        ocrReviewConfirm.setOnClickListener { viewModel.onEvent(OcrReviewEvent.ConfirmClicked) }
        ocrReviewDocumentText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.onEvent(OcrReviewEvent.DocumentTextChanged(s?.toString().orEmpty()))
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.effects.collect(::handleEffect) }
            }
        }
    }

    private fun render(state: OcrReviewUiState) = with(binding) {
        if (!isOcrSessionValid()) { root.visibility = android.view.View.INVISIBLE; return@with }
        if (ocrReviewDocumentText.text.toString() != state.documentText) {
            ocrReviewDocumentText.setText(state.documentText)
            ocrReviewDocumentText.setSelection(state.documentText.length)
        }
        ocrReviewWarningTitle.isVisible = state.warnings.isNotEmpty()
        (ocrReviewWarningOneSection.parent.parent as android.view.View).isVisible = state.warnings.isNotEmpty()
        (ocrReviewWarningTwoSection.parent.parent as android.view.View).isVisible = state.warnings.size > 1
        state.warnings.getOrNull(0)?.let { warning ->
            renderWarning(warning, ocrReviewWarningOneSection, ocrReviewWarningOneConfidence, ocrReviewWarningOneRecognized, ocrReviewWarningOneSource)
        }
        state.warnings.getOrNull(1)?.let { warning ->
            renderWarning(warning, ocrReviewWarningTwoSection, ocrReviewWarningTwoConfidence, ocrReviewWarningTwoRecognized, ocrReviewWarningTwoSource)
        }
    }

    private fun renderWarning(
        warning: OcrReviewWarning,
        section: android.widget.TextView,
        confidence: android.widget.TextView,
        recognized: android.widget.TextView,
        source: android.widget.TextView
    ) {
        section.text = warning.section
        confidence.text = warning.confidence
        val recognizedValue = warning.recognizedValue
        val renderedRecognized = getString(R.string.ocr_review_recognized_text, recognizedValue)
        recognized.text = Spannable.Factory.getInstance().newSpannable(renderedRecognized).apply {
            setSpan(
                StyleSpan(android.graphics.Typeface.BOLD),
                renderedRecognized.length - recognizedValue.length,
                renderedRecognized.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        source.text = warning.source
    }

    private fun handleEffect(effect: OcrReviewEffect) {
        if (!isOcrSessionValid()) return
        when (effect) {
            OcrReviewEffect.NavigateBack -> finish()
            is OcrReviewEffect.FinishWithConfirmedReview -> {
                setResult(RESULT_OK, Intent()
                    .putExtra(EXTRA_REVIEW_CONFIRMED, true)
                    .putExtra(EXTRA_DOCUMENT_ID, effect.documentId)
                    .putExtra(EXTRA_CONTRACT_ID, effect.contractId)
                    .putExtra(EXTRA_DOCUMENT_TEXT, effect.documentText))
                finish()
            }
        }
    }

    companion object {
        const val EXTRA_REVIEW_CONFIRMED = "extra_review_confirmed"
        const val EXTRA_DOCUMENT_ID = "ocr_review_document_id"
        const val EXTRA_CONTRACT_ID = "ocr_review_contract_id"
        const val EXTRA_DOCUMENT_TEXT = "ocr_review_document_text"

        private const val EXTRA_WARNING_SECTIONS = "ocr_review_warning_sections"
        private const val EXTRA_WARNING_CONFIDENCES = "ocr_review_warning_confidences"
        private const val EXTRA_WARNING_VALUES = "ocr_review_warning_values"
        private const val EXTRA_WARNING_SOURCES = "ocr_review_warning_sources"

        private fun warningsFrom(intent: Intent): List<OcrReviewWarning> {
            val sections = intent.getStringArrayListExtra(EXTRA_WARNING_SECTIONS).orEmpty()
            val confidences = intent.getStringArrayListExtra(EXTRA_WARNING_CONFIDENCES).orEmpty()
            val values = intent.getStringArrayListExtra(EXTRA_WARNING_VALUES).orEmpty()
            val sources = intent.getStringArrayListExtra(EXTRA_WARNING_SOURCES).orEmpty()
            return sections.indices.map { OcrReviewWarning(sections[it], confidences[it], values[it], sources[it]) }
        }

        fun createIntent(context: Context, documentId: String = "", contractId: String? = null, documentText: String = "", warnings: List<com.example.lagallens.domain.model.ocr.OcrWarning> = emptyList()): Intent =
            com.example.lagallens.presentation.feature.ocr.OcrSession.stamp(context, Intent(context, OcrReviewActivity::class.java)
                .putExtra(EXTRA_DOCUMENT_ID, documentId)
                .putExtra(EXTRA_CONTRACT_ID, contractId)
                .putExtra(EXTRA_DOCUMENT_TEXT, documentText)
                .putStringArrayListExtra(EXTRA_WARNING_SECTIONS, ArrayList(warnings.map { it.section }))
                .putStringArrayListExtra(EXTRA_WARNING_CONFIDENCES, ArrayList(warnings.map { it.confidence }))
                .putStringArrayListExtra(EXTRA_WARNING_VALUES, ArrayList(warnings.map { it.recognizedValue }))
                .putStringArrayListExtra(EXTRA_WARNING_SOURCES, ArrayList(warnings.map { it.source })))
    }
}
