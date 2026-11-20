package com.example.lagallens.presentation.feature.creditstore.locked.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityLockedResultsBinding
import com.example.lagallens.domain.usecase.GetLockedAnalysisResultsUseCase
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import com.example.lagallens.presentation.feature.analysis.processing.ui.AnalysisProcessingActivity
import com.example.lagallens.presentation.feature.analysis.result.ui.AnalysisResultActivity
import com.example.lagallens.presentation.feature.creditstore.locked.adapter.LockedResultsAdapter
import com.example.lagallens.presentation.feature.creditstore.locked.contract.LockedResultsUiEffect
import com.example.lagallens.presentation.feature.creditstore.locked.contract.LockedResultsUiEvent
import com.example.lagallens.presentation.feature.creditstore.locked.contract.LockedResultsUiState
import com.example.lagallens.presentation.feature.creditstore.locked.viewmodel.LockedResultsViewModel
import kotlinx.coroutines.launch

/** Analysis results the user has not unlocked; opening one shows its free preview and the unlock action. */
class LockedResultsActivity : AppCompatActivity() {
    private val binding by lazy { ActivityLockedResultsBinding.inflate(layoutInflater) }
    private val adapter = LockedResultsAdapter { viewModel.onEvent(LockedResultsUiEvent.ResultClicked(it)) }
    private val viewModel by viewModels<LockedResultsViewModel> {
        val app = application as LegalLensApplication
        LockedResultsViewModel.Factory(
            GetLockedAnalysisResultsUseCase(app.contractRepository, app.analysisIdentityRepository, app.analysisUnlockRepository)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!viewModel.isOwnerSession()) {
            finish()
            return
        }
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.lockedResultsRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        listOf(binding.lockedResultsBack, binding.btnLockedResultsRetry).forEach { it.bindPressFeedback() }
        binding.rvLockedResults.layoutManager = LinearLayoutManager(this)
        binding.rvLockedResults.adapter = adapter
        binding.lockedResultsBack.setOnClickListener { viewModel.onEvent(LockedResultsUiEvent.BackClicked) }
        binding.btnLockedResultsRetry.setOnClickListener { viewModel.onEvent(LockedResultsUiEvent.RetryClicked) }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = viewModel.onEvent(LockedResultsUiEvent.BackClicked)
        })
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.uiEffect.collect(::handleEffect) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!viewModel.isOwnerSession()) finish() else viewModel.onEvent(LockedResultsUiEvent.ScreenResumed)
    }

    private fun render(state: LockedResultsUiState) = with(binding) {
        val messageRes = when {
            state.errorMessageRes != null -> state.errorMessageRes
            !state.isLoading && state.results.isEmpty() -> R.string.locked_results_empty
            else -> null
        }
        pbLockedResults.isVisible = state.isLoading && state.results.isEmpty()
        rvLockedResults.isVisible = state.results.isNotEmpty()
        groupLockedResultsMessage.isVisible = messageRes != null
        if (messageRes != null) tvLockedResultsMessage.setText(messageRes)
        btnLockedResultsRetry.visibility = if (state.errorMessageRes != null) View.VISIBLE else View.GONE
        adapter.submitList(state.results)
    }

    private fun handleEffect(effect: LockedResultsUiEffect) {
        when (effect) {
            LockedResultsUiEffect.NavigateBack -> finish()
            is LockedResultsUiEffect.OpenResult -> startActivity(
                AnalysisResultActivity.createIntent(this, effect.result.contractTitle, effect.result.fileName)
                    .putExtra(AnalysisProcessingActivity.EXTRA_RESULT_ID, effect.result.resultId)
                    .putExtra(AnalysisProcessingActivity.EXTRA_SESSION_KEY, effect.sessionKey)
            )
        }
    }

    companion object {
        fun createIntent(context: Context): Intent = Intent(context, LockedResultsActivity::class.java)
    }
}
