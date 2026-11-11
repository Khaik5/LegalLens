package com.example.lagallens.presentation.feature.creditstore.unlock.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityUnlockConfirmationBinding
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import com.example.lagallens.presentation.feature.creditstore.unlock.contract.UnlockConfirmationUiEffect
import com.example.lagallens.presentation.feature.creditstore.unlock.contract.UnlockConfirmationUiEvent
import com.example.lagallens.presentation.feature.creditstore.unlock.contract.UnlockConfirmationUiState
import com.example.lagallens.presentation.feature.creditstore.unlock.dialog.UnlockProcessingDialog
import com.example.lagallens.presentation.feature.creditstore.unlock.dialog.UnlockSuccessDialog
import com.example.lagallens.presentation.feature.creditstore.unlock.viewmodel.UnlockConfirmationViewModel
import com.example.lagallens.presentation.feature.creditstore.flow.ui.UnlockedAnalysisResultActivity
import com.example.lagallens.presentation.feature.creditstore.flow.ui.InsufficientCreditsActivity
import kotlinx.coroutines.launch
import com.example.lagallens.LegalLensApplication

class UnlockConfirmationActivity : AppCompatActivity() {
    private val binding by lazy { ActivityUnlockConfirmationBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<UnlockConfirmationViewModel> {
        UnlockConfirmationViewModel.Factory((application as LegalLensApplication).creditRepository,
            (application as LegalLensApplication).analysisUnlockRepository)
    }
    private val owner by lazy { intent.getStringExtra(EXTRA_OWNER).orEmpty() }
    private fun isOwner() = owner.isNotBlank() && (application as LegalLensApplication).creditRepository.activeSessionKey() == owner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.alpha = 0f
        if (!isOwner() || intent.getStringExtra(EXTRA_ANALYSIS_RESULT_ID).isNullOrBlank()) { finish(); return }
        enableEdgeToEdge()
        setContentView(binding.root)
        lifecycleScope.launch { (application as LegalLensApplication).creditRepository.observeSessionKeys().collect {
            if (!isOwner()) { window.decorView.alpha = 0f; finish() }
        } }
        supportFragmentManager.setFragmentResultListener(UnlockSuccessDialog.RESULT_KEY, this) { _, _ ->
            // The result screen below refreshes its entitlement when it resumes.
            finish()
        }
        supportFragmentManager.setFragmentResultListener(UnlockProcessingDialog.RESULT_KEY, this) { _, _ ->
            if (isOwner()) showUnlockSuccessDialog()
        }
        bindView()
        collectViewModel()
        viewModel.onEvent(
            UnlockConfirmationUiEvent.ScreenOpened(
                analysisResultId = intent.getStringExtra(EXTRA_ANALYSIS_RESULT_ID).orEmpty(),
                availableCredits = intent.getIntExtra(EXTRA_AVAILABLE_CREDITS, 0),
                unlockCost = intent.getIntExtra(EXTRA_UNLOCK_CREDIT_COST, 0)
            )
        )
        bindInsets()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                viewModel.onEvent(UnlockConfirmationUiEvent.BackClicked)
            }
        })
    }

    override fun onResume() { super.onResume(); if (!isOwner()) { window.decorView.alpha = 0f; finish() } else window.decorView.alpha = 1f }
    override fun onPause() { window.decorView.alpha = 0f; super.onPause() }

    private fun bindView() = with(binding) {
        listOf(unlockConfirmationBack, unlockConfirmationCancel, unlockConfirmationConfirm).forEach { it.bindPressFeedback() }
        unlockConfirmationBack.setOnClickListener {
            viewModel.onEvent(UnlockConfirmationUiEvent.BackClicked)
        }
        unlockConfirmationCancel.setOnClickListener {
            viewModel.onEvent(UnlockConfirmationUiEvent.CancelClicked)
        }
        unlockConfirmationConfirm.setOnClickListener {
            viewModel.onEvent(UnlockConfirmationUiEvent.ConfirmClicked)
        }
    }

    private fun collectViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.uiEffect.collect(::handleEffect) }
            }
        }
    }

    private fun bindInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.unlockConfirmationRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun render(state: UnlockConfirmationUiState) = with(binding) {
        val contractName = intent.getStringExtra(EXTRA_CONTRACT_NAME).orEmpty()
        unlockConfirmationContractName.text = contractName
        unlockConfirmationContractLabel.isVisible = contractName.isNotBlank()
        unlockConfirmationContractName.isVisible = contractName.isNotBlank()
        unlockConfirmationResultMeta.text = getString(
            R.string.unlock_confirmation_result_meta,
            state.analysisResultId
        )
        unlockConfirmationBalanceBefore.text = getString(
            R.string.unlock_confirmation_credit_value,
            state.availableCredits
        )
        unlockConfirmationCost.text = getString(
            R.string.unlock_confirmation_credit_deduction,
            state.unlockCost
        )
        unlockConfirmationBalanceAfter.text = getString(
            R.string.unlock_confirmation_credit_value,
            state.remainingCredits
        )
        unlockConfirmationConfirm.text = getString(
            R.string.unlock_confirmation_confirm,
            state.unlockCost
        )
        unlockConfirmationConfirm.isEnabled =
            !state.isConfirmationRequested && !state.isInsufficientCreditsRequested
        if (state.isBackendUnavailable) {
            unlockConfirmationBalanceBefore.setText(R.string.credit_backend_not_configured)
            unlockConfirmationBalanceAfter.setText(R.string.credit_backend_not_configured)
        }
    }

    private fun handleEffect(effect: UnlockConfirmationUiEffect) {
        if (!isOwner()) return
        when (effect) {
            UnlockConfirmationUiEffect.ShowBackendUnavailable -> Toast.makeText(this, R.string.credit_backend_not_configured, Toast.LENGTH_SHORT).show()
            UnlockConfirmationUiEffect.NavigateBack -> finish()
            UnlockConfirmationUiEffect.NavigateToInsufficientCredits -> {
                val state = viewModel.uiState.value
                startActivity(
                    InsufficientCreditsActivity.createIntent(
                        this,
                        fromAnalysis = intent.getBooleanExtra(EXTRA_FROM_ANALYSIS, false),
                        contractName = intent.getStringExtra(EXTRA_CONTRACT_NAME),
                        requiredCredits = state.unlockCost,
                        availableCredits = state.availableCredits
                    )
                )
                finish()
            }
            is UnlockConfirmationUiEffect.UnlockRequested -> {
                showUnlockProcessingDialog()
            }
        }
    }

    private fun showUnlockProcessingDialog() {
        if (supportFragmentManager.findFragmentByTag(UnlockProcessingDialog.TAG) == null) {
            UnlockProcessingDialog().show(supportFragmentManager, UnlockProcessingDialog.TAG)
        }
    }

    private fun showUnlockSuccessDialog() {
        if (supportFragmentManager.findFragmentByTag(UnlockSuccessDialog.TAG) == null) {
            UnlockSuccessDialog().show(supportFragmentManager, UnlockSuccessDialog.TAG)
        }
    }

    companion object {
        const val EXTRA_ANALYSIS_RESULT_ID = "extra_analysis_result_id"
        const val EXTRA_AVAILABLE_CREDITS = "extra_available_credits"
        const val EXTRA_UNLOCK_CREDIT_COST = "extra_unlock_credit_cost"
        private const val EXTRA_OWNER = "unlock_owner_session"
        private const val EXTRA_CONTRACT_NAME = "unlock_contract_name"
        private const val EXTRA_FROM_ANALYSIS = "unlock_from_analysis"

        fun createIntent(
            context: Context,
            analysisResultId: String,
            availableCredits: Int,
            unlockCost: Int,
            contractName: String? = null,
            fromAnalysis: Boolean = false
        ): Intent = Intent(context, UnlockConfirmationActivity::class.java)
            .putExtra(EXTRA_CONTRACT_NAME, contractName)
            .putExtra(EXTRA_FROM_ANALYSIS, fromAnalysis)
            .putExtra(EXTRA_ANALYSIS_RESULT_ID, analysisResultId)
            .putExtra(EXTRA_AVAILABLE_CREDITS, availableCredits)
            .putExtra(EXTRA_UNLOCK_CREDIT_COST, unlockCost)
            .putExtra(EXTRA_OWNER, (context.applicationContext as LegalLensApplication).creditRepository.activeSessionKey())
    }
}
