package com.example.lagallens.presentation.feature.creditstore.flow.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.lagallens.R
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.databinding.ActivityPaymentPendingBinding
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEffect
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PaymentPendingActivity : CreditFlowActivity() {
    private val binding by lazy { ActivityPaymentPendingBinding.inflate(layoutInflater) }
    private lateinit var selection: CreditPurchaseSelection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selection = CreditPurchaseSelection.fromIntent(intent) ?: run { finish(); return }
        enableEdgeToEdge()
        setContentView(binding.root)
        binding.pendingPackage.setText(selection.creditPackage.titleRes)
        binding.pendingPrice.text = selection.priceText
        val creditRepository = (application as LegalLensApplication).creditRepository
        binding.pendingBalance.text = if (creditRepository.isConfigured) getString(R.string.credit_store_balance_credits, creditRepository.balance.value)
        else getString(R.string.credit_backend_not_configured)
        binding.pendingCredits.text = getString(R.string.credit_purchase_added, selection.creditPackage.credits)
        binding.pendingCreditsNote.text = getString(R.string.credit_purchase_pending_note, selection.creditPackage.credits)
        viewModel.onEvent(CreditFlowUiEvent.StartPayment(selection.purchaseId, selection.creditPackage.id, selection.creditPackage.priceVnd.toLong(), selection.creditPackage.credits))
        binding.paymentPendingBack.setOnClickListener {
            viewModel.onEvent(CreditFlowUiEvent.BackClicked)
        }
        applySystemBarInsets(binding.paymentPendingRoot)
        bindSystemBack()
        collectEffects(::handleEffect)
        lifecycleScope.launch {
            delay(PAYMENT_DEMO_DELAY_MS)
            if (!isFinishing) {
                viewModel.onEvent(CreditFlowUiEvent.CompletePayment(selection.purchaseId))
            }
        }
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
        private const val PAYMENT_DEMO_DELAY_MS = 1500L

        internal fun createIntent(context: Context, selection: CreditPurchaseSelection): Intent = selection.putInto(Intent(context, PaymentPendingActivity::class.java))
    }
}
