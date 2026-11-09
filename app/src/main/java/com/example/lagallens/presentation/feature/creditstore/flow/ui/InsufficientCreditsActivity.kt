package com.example.lagallens.presentation.feature.creditstore.flow.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
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
        fun createIntent(context: Context, fromAnalysis: Boolean = false): Intent = Intent(context, InsufficientCreditsActivity::class.java).putExtra(EXTRA_FROM_ANALYSIS, fromAnalysis)
        internal fun purchaseCompletedIntent(context: Context): Intent = createIntent(context, true)
            .putExtra(EXTRA_PURCHASE_COMPLETED, true)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}
