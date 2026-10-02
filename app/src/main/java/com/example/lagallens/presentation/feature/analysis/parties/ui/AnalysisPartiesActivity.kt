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
import com.example.lagallens.presentation.feature.analysis.parties.contract.AnalysisParty
import com.example.lagallens.presentation.feature.analysis.parties.viewmodel.AnalysisPartiesViewModel
import kotlinx.coroutines.launch

class AnalysisPartiesActivity : AppCompatActivity() {
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

    private fun render(state: AnalysisPartiesUiState) = with(binding) {
        analysisPartiesDocumentName.text = state.documentName
        analysisPartiesDocumentMetadata.text = state.documentMetadata
        renderEmployer(state.employer)
        renderEmployee(state.employee)
    }

    private fun renderEmployer(party: AnalysisParty) = with(binding) {
        analysisPartiesEmployerTitle.setText(party.titleRes)
        analysisPartiesEmployerRole.setText(party.roleRes)
        analysisPartiesEmployerFirstLabel.setText(party.firstLabelRes)
        analysisPartiesEmployerFirstValue.setText(party.firstValueRes)
        analysisPartiesEmployerSecondLabel.setText(party.secondLabelRes)
        analysisPartiesEmployerSecondValue.setText(party.secondValueRes)
        analysisPartiesEmployerThirdLabel.setText(party.thirdLabelRes)
        analysisPartiesEmployerThirdValue.setText(party.thirdValueRes)
        analysisPartiesEmployerFourthLabel.setText(party.fourthLabelRes)
        analysisPartiesEmployerFourthValue.setText(party.fourthValueRes)
    }

    private fun renderEmployee(party: AnalysisParty) = with(binding) {
        analysisPartiesEmployeeTitle.setText(party.titleRes)
        analysisPartiesEmployeeRole.setText(party.roleRes)
        analysisPartiesEmployeeFirstLabel.setText(party.firstLabelRes)
        analysisPartiesEmployeeFirstValue.setText(party.firstValueRes)
        analysisPartiesEmployeeSecondLabel.setText(party.secondLabelRes)
        analysisPartiesEmployeeSecondValue.setText(party.secondValueRes)
        analysisPartiesEmployeeThirdLabel.setText(party.thirdLabelRes)
        analysisPartiesEmployeeThirdValue.setText(party.thirdValueRes)
        analysisPartiesEmployeeFourthLabel.setText(party.fourthLabelRes)
        analysisPartiesEmployeeFourthValue.setText(party.fourthValueRes)
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
