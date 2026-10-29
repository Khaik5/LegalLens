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
            binding.successHeading.text = getString(R.string.credit_flow_success_heading, payment.credits)
            binding.successCredits.text = getString(R.string.credit_purchase_added, payment.credits)
            binding.successPrice.text = selection.priceText
            binding.successBalanceBefore.text = getString(R.string.credit_store_balance_credits, balance - payment.credits)
            binding.successBalanceAfter.text = getString(R.string.credit_store_balance_credits, balance)
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
                startActivity(CreditHubActivity.createIntent(this).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
                finish()
            }

            else -> Unit
        }
    }

    companion object {
        internal fun createIntent(context: Context, selection: CreditPurchaseSelection): Intent = selection.putInto(Intent(context, PurchaseSuccessActivity::class.java))
    }
}
