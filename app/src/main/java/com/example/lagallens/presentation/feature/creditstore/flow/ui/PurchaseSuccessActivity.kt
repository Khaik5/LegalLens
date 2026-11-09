package com.example.lagallens.presentation.feature.creditstore.flow.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import com.example.lagallens.R
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.databinding.ActivityPurchaseSuccessBinding
import com.example.lagallens.domain.model.credit.PurchaseBalance
import com.example.lagallens.domain.model.credit.purchaseBalance
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEffect
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEvent
import com.example.lagallens.presentation.feature.creditstore.hub.ui.CreditHubActivity

class PurchaseSuccessActivity : CreditFlowActivity() {
    private val binding by lazy { ActivityPurchaseSuccessBinding.inflate(layoutInflater) }
    private lateinit var selection: CreditPurchaseSelection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selection = CreditPurchaseSelection.fromIntent(intent) ?: run { finish(); return }
        enableEdgeToEdge()
        setContentView(binding.root)
        lifecycleScope.launch {
            val app = application as LegalLensApplication
            val payment = runCatching { app.paymentRepository.get(selection.purchaseId) }.getOrNull()
            if (payment == null || payment.status != com.example.lagallens.domain.model.payment.PaymentStatus.SUCCESS) {
                android.widget.Toast.makeText(this@PurchaseSuccessActivity, R.string.auth_backend_not_configured, android.widget.Toast.LENGTH_LONG).show()
                finish()
                return@launch
            }
            val balance = app.creditRepository.balance.value
            val transactions = runCatching { app.creditRepository.getTransactions() }
                .onFailure { if (it is kotlinx.coroutines.CancellationException) throw it }
                .getOrDefault(emptyList())
            val receipt = transactions.purchaseBalance(selection.purchaseId, balance)
                ?: PurchaseBalance(balance - payment.credits, balance)
            binding.successHeading.text = getString(R.string.credit_flow_success_heading, payment.credits)
            binding.successCredits.text = getString(R.string.credit_purchase_added, payment.credits)
            binding.successPrice.text = selection.priceText
            binding.successTransaction.text = getString(
                R.string.credit_success_transaction,
                selection.transactionReference,
                java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT).format(java.util.Date(payment.updatedAtEpochMillis))
            )
            binding.successBalanceBefore.text = getString(R.string.credit_store_balance_credits, receipt.before)
            binding.successBalanceAfter.text = getString(R.string.credit_store_balance_credits, receipt.after)
        }
        binding.purchaseSuccessReturnToUnlock.bindPressFeedback()
        binding.purchaseSuccessBack.setOnClickListener {
            viewModel.onEvent(CreditFlowUiEvent.BackClicked)
        }
        binding.purchaseSuccessReturnToUnlock.setOnClickListener {
            viewModel.onEvent(CreditFlowUiEvent.BackClicked)
        }
        applySystemBarInsets(binding.purchaseSuccessRoot)
        bindSystemBack()
        collectEffects(::handleEffect)
    }

    private fun handleEffect(effect: CreditFlowUiEffect) {
        when (effect) {
            CreditFlowUiEffect.NavigateBack -> {
                if (selection.returnsToUnlockOrigin) startActivity(InsufficientCreditsActivity.purchaseCompletedIntent(this))
                else startActivity(CreditHubActivity.createIntent(this).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
                finish()
            }

            else -> Unit
        }
    }

    companion object {
        internal fun createIntent(context: Context, selection: CreditPurchaseSelection): Intent = selection.putInto(Intent(context, PurchaseSuccessActivity::class.java))
    }
}
