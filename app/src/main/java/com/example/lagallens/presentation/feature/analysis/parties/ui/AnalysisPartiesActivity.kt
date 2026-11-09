package com.example.lagallens.presentation.feature.analysis.parties.ui

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
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityAnalysisPartiesBinding
import com.example.lagallens.presentation.feature.analysis.parties.contract.AnalysisPartiesUiEffect
import com.example.lagallens.presentation.feature.analysis.parties.contract.AnalysisPartiesUiEvent
import com.example.lagallens.presentation.feature.analysis.parties.contract.AnalysisPartiesUiState
import com.example.lagallens.presentation.feature.analysis.parties.viewmodel.AnalysisPartiesViewModel
import com.example.lagallens.presentation.feature.analysis.adapter.bindLockedPreview
import kotlinx.coroutines.launch

class AnalysisPartiesActivity : com.example.lagallens.presentation.feature.analysis.AnalysisScopedActivity() {
    override val requiresUnlock = false
    override val observesUnlock = true
    private lateinit var binding: ActivityAnalysisPartiesBinding
    private val viewModel by viewModels<AnalysisPartiesViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityAnalysisPartiesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.analysisPartiesRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.analysisPartiesBack.setOnClickListener {
            viewModel.onEvent(AnalysisPartiesUiEvent.BackClicked)
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                viewModel.onEvent(AnalysisPartiesUiEvent.BackClicked)
            }
        })
        observeViewModel()
        viewModel.onEvent(
            AnalysisPartiesUiEvent.ScreenOpened(
                documentName = intent.getStringExtra(EXTRA_DOCUMENT_NAME)
                    .orEmpty(),
                documentMetadata = intent.getStringExtra(EXTRA_DOCUMENT_METADATA)
                    .orEmpty()
            )
        )
    }

    override fun onAnalysisInvalidated() { viewModel.clearResult() }

    override fun onAnalysisValidated(identity: com.example.lagallens.domain.model.analysis.AnalysisIdentity, detail: com.example.lagallens.domain.model.analysis.AnalysisDetail) {
        viewModel.setResult(identity, detail, validatedUnlockStatus?.isUnlocked == true)
        showContentStatus(viewModel.uiState.value.status)
        render(viewModel.uiState.value)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.uiEffect.collect(::handleEffect) }
            }
        }
    }

    private fun render(state: AnalysisPartiesUiState) = with(binding) {
        analysisContentEmpty.visibility = if (state.status == com.example.lagallens.presentation.feature.analysis.SnapshotContentStatus.EMPTY) android.view.View.VISIBLE else android.view.View.GONE
        analysisPartiesDocumentName.text = state.documentName
        analysisPartiesDocumentMetadata.text = getString(R.string.analysis_snapshot_demo_metadata, state.documentMetadata)
        analysisPartiesItems.removeAllViews()
        state.parties.forEachIndexed { index, party ->
            val card = layoutInflater.inflate(R.layout.item_analysis_party, analysisPartiesItems, false)
            val badge = card.findViewById<android.widget.TextView>(R.id.analysis_party_badge)
            badge.text = if (index < 26) ('A'.code + index).toChar().toString() else (index + 1).toString()
            if (index % 2 == 1) {
                badge.setBackgroundResource(R.drawable.bg_analysis_party_employee_badge)
                card.findViewById<android.widget.TextView>(R.id.analysis_party_role).apply {
                    setBackgroundResource(R.drawable.bg_analysis_party_employee_chip)
                    setTextColor(getColor(R.color.legal_lens_secondary_text))
                }
            }
            card.findViewById<android.widget.TextView>(R.id.analysis_party_title).text = party.title
            card.findViewById<android.widget.TextView>(R.id.analysis_party_role).text = party.role
            val fields = card.findViewById<android.widget.LinearLayout>(R.id.analysis_party_fields)
            party.fields.forEach { field ->
                val row = layoutInflater.inflate(R.layout.item_analysis_party_field, fields, false)
                row.findViewById<android.widget.TextView>(R.id.analysis_party_field_label).text = field.label
                row.findViewById<android.widget.TextView>(R.id.analysis_party_field_value).text = field.value
                fields.addView(row)
            }
            analysisPartiesItems.addView(card)
        }
        if (state.lockedCount > 0) {
            val locked = com.example.lagallens.databinding.ItemAnalysisLockedPreviewBinding.inflate(layoutInflater, analysisPartiesItems, false)
            locked.bindLockedPreview(state.lockedCount, validatedUnlockStatus?.cost) { openUnlockConfirmation(state.documentName) }
            analysisPartiesItems.addView(locked.root)
        }
    }

    private fun handleEffect(effect: AnalysisPartiesUiEffect) {
        when (effect) {
            AnalysisPartiesUiEffect.NavigateBack -> finish()
        }
    }

    companion object {
        private const val EXTRA_DOCUMENT_NAME = "analysis_parties_document_name"
        private const val EXTRA_DOCUMENT_METADATA = "analysis_parties_document_metadata"

        fun createIntent(
            context: Context,
            documentName: String,
            documentMetadata: String
        ): Intent = Intent(context, AnalysisPartiesActivity::class.java)
            .putExtra(EXTRA_DOCUMENT_NAME, documentName)
            .putExtra(EXTRA_DOCUMENT_METADATA, documentMetadata)
    }
}
