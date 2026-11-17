package com.example.lagallens.presentation.feature.analysis.legalreferences.detail.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.example.lagallens.R
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.databinding.ActivityAnalysisLegalReferenceDetailBinding
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import com.example.lagallens.presentation.feature.analysis.legalreferences.detail.contract.AnalysisLegalReferenceDetailUiEffect
import com.example.lagallens.presentation.feature.analysis.legalreferences.detail.contract.AnalysisLegalReferenceDetailUiEvent
import com.example.lagallens.presentation.feature.analysis.legalreferences.detail.contract.AnalysisLegalReferenceDetailUiState
import com.example.lagallens.presentation.feature.analysis.legalreferences.detail.viewmodel.AnalysisLegalReferenceDetailViewModel
import com.example.lagallens.presentation.feature.analysis.legalreferences.model.LegalReferenceFormat
import com.example.lagallens.presentation.feature.analysis.legalreferences.model.toLegalReference
import com.example.lagallens.presentation.feature.analysis.risk.detail.ui.AnalysisRiskDetailActivity
import kotlinx.coroutines.launch

class AnalysisLegalReferenceDetailActivity : com.example.lagallens.presentation.feature.analysis.AnalysisScopedActivity() {
    override val requiresUnlock = false
    override val observesUnlock = true
    private lateinit var binding: ActivityAnalysisLegalReferenceDetailBinding
    private val viewModel by viewModels<AnalysisLegalReferenceDetailViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityAnalysisLegalReferenceDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.visibility = android.view.View.INVISIBLE
        ViewCompat.setOnApplyWindowInsetsListener(binding.analysisLegalReferenceDetailRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        bindEvents()
        observeViewModel()
    }

    override fun onAnalysisInvalidated() { if (::binding.isInitialized) binding.root.visibility = android.view.View.INVISIBLE }

    override fun onAnalysisValidated(identity: com.example.lagallens.domain.model.analysis.AnalysisIdentity, detail: com.example.lagallens.domain.model.analysis.AnalysisDetail) {
        val referenceId = intent.getStringExtra(EXTRA_REFERENCE_ID).orEmpty()
        val isDemo = com.example.lagallens.di.DataModeConfig.current == com.example.lagallens.di.AppDataMode.DEMO
        val backendReference = if (isDemo) null else visibleCitation(detail, referenceId)
        if (if (isDemo) !com.example.lagallens.presentation.feature.analysis.legalreferences.model.DemoLegalReferences.canView(referenceId, validatedUnlockStatus?.isUnlocked == true) else backendReference == null) {
            android.widget.Toast.makeText(this, R.string.analysis_citation_unavailable, android.widget.Toast.LENGTH_LONG).show()
            finish()
            return
        }
        viewModel.onEvent(
            AnalysisLegalReferenceDetailUiEvent.ScreenOpened(
                referenceId = intent.getStringExtra(EXTRA_REFERENCE_ID).orEmpty(),
                documentName = intent.getStringExtra(EXTRA_DOCUMENT_NAME).orEmpty(),
                documentMetadata = intent.getStringExtra(EXTRA_DOCUMENT_METADATA).orEmpty()
            )
        )
        backendReference?.let { viewModel.setReference(it.toLegalReference().text!!) }
        binding.root.visibility = android.view.View.VISIBLE
    }

    /** A locked citation is never opened, even if its ID is known. */
    private fun visibleCitation(detail: com.example.lagallens.domain.model.analysis.AnalysisDetail, referenceId: String): com.example.lagallens.domain.model.analysis.AnalysisCitation? {
        val visible = com.example.lagallens.domain.model.analysis.AnalysisPreview.visibleCount(detail.citations.size, validatedUnlockStatus?.isUnlocked == true)
        return detail.citations.take(visible).firstOrNull { it.id == referenceId }
    }

    private fun bindEvents() = with(binding) {
        listOf(analysisLegalReferenceDetailBack, analysisLegalReferenceDetailRisk).forEach { it.bindPressFeedback() }
        analysisLegalReferenceDetailBack.setOnClickListener {
            viewModel.onEvent(AnalysisLegalReferenceDetailUiEvent.BackClicked)
        }
        analysisLegalReferenceDetailRisk.setOnClickListener {
            viewModel.onEvent(AnalysisLegalReferenceDetailUiEvent.RelatedRiskClicked)
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

    private fun render(state: AnalysisLegalReferenceDetailUiState) = with(binding) {
        state.text?.let { renderBackend(it); return@with }
        analysisLegalReferenceDetailLawTitle.setText(state.titleRes)
        analysisLegalReferenceDetailArticle.setText(state.articleRes)
        analysisLegalReferenceDetailEffectiveDate.setText(state.effectiveDateRes)
        analysisLegalReferenceDetailFullText.setText(state.fullTextRes)
        analysisLegalReferenceDetailTextHeading.setText(R.string.analysis_legal_demo_notice)
        analysisLegalReferenceDetailMetadata.isVisible = false
        analysisLegalReferenceDetailRiskHeading.isVisible = false
        analysisLegalReferenceDetailRisk.isVisible = false
        analysisLegalReferenceDetailIssuerValue.setText(state.issuerRes)
        analysisLegalReferenceDetailDocumentTypeValue.setText(state.documentTypeRes)
        analysisLegalReferenceDetailStatus.setText(state.statusRes)
        analysisLegalReferenceDetailRiskTitle.setText(state.relatedRiskTitleRes)
        analysisLegalReferenceDetailRiskSubtitle.setText(state.relatedRiskSubtitleRes)
    }

    private fun renderBackend(text: com.example.lagallens.presentation.feature.analysis.legalreferences.model.AnalysisLegalReferenceText) = with(binding) {
        analysisLegalReferenceDetailLawTitle.text = text.title
        analysisLegalReferenceDetailArticle.text = text.articleNumber?.let { LegalReferenceFormat.article(this@AnalysisLegalReferenceDetailActivity, it) }.orEmpty()
        analysisLegalReferenceDetailEffectiveDate.text = LegalReferenceFormat.effective(this@AnalysisLegalReferenceDetailActivity, text.effectiveDate)
        analysisLegalReferenceDetailFullText.text = text.description
        analysisLegalReferenceDetailTextHeading.setText(R.string.analysis_legal_reference_detail_text_heading)
        // Issuer and document type are not stored by the backend yet, so the block stays hidden.
        analysisLegalReferenceDetailMetadata.isVisible = false
        analysisLegalReferenceDetailRiskHeading.isVisible = false
        analysisLegalReferenceDetailRisk.isVisible = false
        analysisLegalReferenceDetailStatus.text = LegalReferenceFormat.status(this@AnalysisLegalReferenceDetailActivity, text.validityStatus)
    }

    private fun handleEffect(effect: AnalysisLegalReferenceDetailUiEffect) {
        when (effect) {
            AnalysisLegalReferenceDetailUiEffect.NavigateBack -> finish()
            AnalysisLegalReferenceDetailUiEffect.NavigateToRelatedRisk ->
                android.widget.Toast.makeText(this, R.string.analysis_citation_unavailable, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val EXTRA_REFERENCE_ID = "analysis_legal_reference_detail_reference_id"
        private const val EXTRA_DOCUMENT_NAME = "analysis_legal_reference_detail_document_name"
        private const val EXTRA_DOCUMENT_METADATA = "analysis_legal_reference_detail_document_metadata"

        fun createIntent(
            context: Context,
            referenceId: String,
            documentName: String,
            documentMetadata: String
        ): Intent = Intent(context, AnalysisLegalReferenceDetailActivity::class.java)
            .putExtra(EXTRA_REFERENCE_ID, referenceId)
            .putExtra(EXTRA_DOCUMENT_NAME, documentName)
            .putExtra(EXTRA_DOCUMENT_METADATA, documentMetadata)
    }
}
