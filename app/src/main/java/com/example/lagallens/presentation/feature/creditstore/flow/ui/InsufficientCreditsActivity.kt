package com.example.lagallens.presentation.feature.creditstore.flow.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.isVisible
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityInsufficientCreditsBinding
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEffect
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEvent
import com.example.lagallens.presentation.feature.creditstore.ui.CreditStoreActivity

class InsufficientCreditsActivity : CreditFlowActivity() {
    private val binding by lazy { ActivityInsufficientCreditsBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra(EXTRA_PURCHASE_COMPLETED, false)) {
            startActivity(com.example.lagallens.presentation.feature.creditstore.hub.ui.CreditHubActivity.createIntent(this))
            finish()
            return
        }
        enableEdgeToEdge()
        setContentView(binding.root)
        bindCredits()
        listOf(binding.insufficientCreditsBack, binding.insufficientCreditsStore, binding.insufficientCreditsContinue).forEach { it.bindPressFeedback() }
        binding.insufficientCreditsBack.setOnClickListener {
            viewModel.onEvent(CreditFlowUiEvent.BackClicked)
        }
        binding.insufficientCreditsStore.setOnClickListener {
            viewModel.onEvent(CreditFlowUiEvent.OpenCreditStoreClicked)
        }
        binding.insufficientCreditsContinue.setOnClickListener {
            viewModel.onEvent(CreditFlowUiEvent.ContinueFreeClicked)
        }
        applySystemBarInsets(binding.insufficientCreditsRoot)
        bindSystemBack()
        collectEffects(::handleEffect)
    }

    /** Shows the unlock the user just tried; nothing here is sample data. */
    private fun bindCredits() = with(binding) {
        val required = intent.getIntExtra(EXTRA_REQUIRED_CREDITS, 0).coerceAtLeast(0)
        val available = intent.getIntExtra(EXTRA_AVAILABLE_CREDITS, 0).coerceAtLeast(0)
        val missing = (required - available).coerceAtLeast(0)
        val contractName = intent.getStringExtra(EXTRA_CONTRACT_NAME).orEmpty()
        insufficientCreditsDescription.text = getString(R.string.credit_flow_insufficient_description, required, available, missing)
        insufficientCreditsContractName.text = contractName
        insufficientCreditsContractName.isVisible = contractName.isNotBlank()
        insufficientCreditsRequired.text = getString(R.string.unlock_confirmation_credit_value, required)
        insufficientCreditsAvailable.text = getString(R.string.unlock_confirmation_credit_value, available)
        insufficientCreditsMissing.text = getString(R.string.unlock_confirmation_credit_value, missing)
    }

    private fun handleEffect(effect: CreditFlowUiEffect) {
        when (effect) {
            CreditFlowUiEffect.NavigateBack -> finish()
            CreditFlowUiEffect.NavigateToCreditStore -> startActivity(CreditStoreActivity.createIntent(this, intent.getBooleanExtra(EXTRA_FROM_ANALYSIS, false)))
            else -> Unit
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_PURCHASE_COMPLETED, false)) {
            startActivity(com.example.lagallens.presentation.feature.creditstore.hub.ui.CreditHubActivity.createIntent(this))
            finish()
        }
    }

    companion object {
        private const val EXTRA_FROM_ANALYSIS = "insufficient_from_analysis"
        private const val EXTRA_PURCHASE_COMPLETED = "insufficient_purchase_completed"
        private const val EXTRA_CONTRACT_NAME = "insufficient_contract_name"
        private const val EXTRA_REQUIRED_CREDITS = "insufficient_required_credits"
        private const val EXTRA_AVAILABLE_CREDITS = "insufficient_available_credits"

        fun createIntent(
            context: Context,
            fromAnalysis: Boolean = false,
            contractName: String? = null,
            requiredCredits: Int = 0,
            availableCredits: Int = 0
        ): Intent = Intent(context, InsufficientCreditsActivity::class.java)
            .putExtra(EXTRA_FROM_ANALYSIS, fromAnalysis)
            .putExtra(EXTRA_CONTRACT_NAME, contractName)
            .putExtra(EXTRA_REQUIRED_CREDITS, requiredCredits)
            .putExtra(EXTRA_AVAILABLE_CREDITS, availableCredits)
        internal fun purchaseCompletedIntent(context: Context): Intent = createIntent(context, true)
            .putExtra(EXTRA_PURCHASE_COMPLETED, true)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}
