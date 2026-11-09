package com.example.lagallens.presentation.feature.creditstore.flow.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import com.example.lagallens.R
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.databinding.ActivityPaymentPendingBinding
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEffect
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEvent

class PaymentPendingActivity : CreditFlowActivity() {
    private val binding by lazy { ActivityPaymentPendingBinding.inflate(layoutInflater) }
    private lateinit var selection: CreditPurchaseSelection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selection = CreditPurchaseSelection.fromIntent(intent) ?: run { finish(); return }
        enableEdgeToEdge()
        setContentView(binding.root)
        binding.pendingPackage.text = selection.creditPackage.title
        binding.pendingPrice.text = selection.priceText
        binding.pendingPriceSummary.text = selection.priceText
        binding.pendingTransactionId.text = selection.transactionReference
        binding.pendingSubmittedAt.text = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT).format(java.util.Date())
        val creditRepository = (application as LegalLensApplication).creditRepository
        binding.pendingBalance.text = if (creditRepository.isConfigured) getString(R.string.credit_store_balance_credits, creditRepository.balance.value)
        else getString(R.string.credit_backend_not_configured)
        binding.pendingCredits.text = getString(R.string.credit_purchase_added, selection.creditPackage.credits)
        binding.pendingCreditsNote.text = getString(R.string.credit_purchase_pending_note, selection.creditPackage.credits)
        viewModel.onEvent(CreditFlowUiEvent.StartPayment(selection.purchaseId, selection.creditPackage.id))
        binding.paymentPendingBack.setOnClickListener {
            viewModel.onEvent(CreditFlowUiEvent.BackClicked)
        }
        applySystemBarInsets(binding.paymentPendingRoot)
        bindSystemBack()
        collectEffects(::handleEffect)
    }

    private fun handleEffect(effect: CreditFlowUiEffect) {
        when (effect) {
            CreditFlowUiEffect.NavigateBack -> finish()
            is CreditFlowUiEffect.PaymentSucceeded -> {
                startActivity(PurchaseSuccessActivity.createIntent(this, selection))
                finish()
            }
            CreditFlowUiEffect.PaymentFailed -> {
                android.widget.Toast.makeText(this, R.string.auth_backend_not_configured, android.widget.Toast.LENGTH_LONG).show()
                finish()
            }
            else -> Unit
        }
    }

    companion object {
        internal fun createIntent(context: Context, selection: CreditPurchaseSelection): Intent = selection.putInto(Intent(context, PaymentPendingActivity::class.java))
    }
}
