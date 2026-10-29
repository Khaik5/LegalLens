package com.example.lagallens.presentation.feature.creditstore.flow.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityPurchaseConfirmationBinding
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEffect
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEvent
import kotlinx.coroutines.launch
import com.example.lagallens.LegalLensApplication

class PurchaseConfirmationActivity : CreditFlowActivity() {
    private val binding by lazy { ActivityPurchaseConfirmationBinding.inflate(layoutInflater) }
    private lateinit var selection: CreditPurchaseSelection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selection = CreditPurchaseSelection.fromIntent(intent) ?: run { finish(); return }
        enableEdgeToEdge()
        setContentView(binding.root)
        val creditPackage = selection.creditPackage
        val price = selection.priceText
        binding.purchasePackageTitle.setText(creditPackage.titleRes)
        binding.purchaseSummaryPackage.setText(creditPackage.titleRes)
        binding.purchasePackageDescription.setText(if (creditPackage.credits == 15) R.string.credit_flow_package_description else R.string.credit_purchase_no_expiry)
        binding.purchasePackagePrice.text = price
        binding.purchaseSummaryPrice.text = price
        val creditRepository = (application as LegalLensApplication).creditRepository
        val currentBalance = creditRepository.balance.value
        val balance = if (creditRepository.isConfigured) getString(R.string.credit_store_balance_credits, currentBalance)
        else getString(R.string.credit_backend_not_configured)
        binding.purchaseBalance.text = balance
        binding.purchaseSummaryBalance.text = balance
        binding.purchaseBalanceAfter.text = if (creditRepository.isConfigured) getString(R.string.credit_store_balance_credits, currentBalance + creditPackage.credits)
        else getString(R.string.credit_backend_not_configured)
        binding.purchaseConfirmationPay.text = getString(R.string.credit_purchase_pay_amount, price)
        listOf(binding.purchaseConfirmationBack, binding.purchaseConfirmationPay).forEach { it.bindPressFeedback() }
        binding.purchaseConfirmationBack.setOnClickListener {
            viewModel.onEvent(CreditFlowUiEvent.BackClicked)
        }
        binding.purchaseConfirmationPay.setOnClickListener {
            viewModel.onEvent(CreditFlowUiEvent.RequestPaymentClicked)
        }
        applySystemBarInsets(binding.purchaseConfirmationRoot)
        bindSystemBack()
        collectState()
        collectEffects(::handleEffect)
    }

    private fun collectState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.purchaseConfirmationPay.isEnabled = !state.isPaymentRequesting && !state.isBackendUnavailable
                }
            }
        }
    }

    private fun handleEffect(effect: CreditFlowUiEffect) {
        when (effect) {
            CreditFlowUiEffect.NavigateBack -> finish()
            CreditFlowUiEffect.PaymentRequested -> {
                startActivity(PaymentPendingActivity.createIntent(this, selection))
                finish()
            }

            else -> Unit
        }
    }

    companion object {
        fun createIntent(context: Context, packageId: String, balanceCredits: Int): Intent {
            val selected = requireNotNull(com.example.lagallens.presentation.feature.creditstore.contract.CreditPackage.samples.firstOrNull { it.id == packageId })
            return CreditPurchaseSelection(selected, balanceCredits).putInto(Intent(context, PurchaseConfirmationActivity::class.java))
        }
    }
}
