package com.example.lagallens.presentation.feature.creditstore.flow.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.lagallens.databinding.ActivityPaymentPendingBinding
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEffect
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PaymentPendingActivity : CreditFlowActivity() {
    private val binding by lazy { ActivityPaymentPendingBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        binding.paymentPendingBack.setOnClickListener {
            viewModel.onEvent(CreditFlowUiEvent.BackClicked)
        }
        applySystemBarInsets(binding.paymentPendingRoot)
        bindSystemBack()
        collectEffects(::handleEffect)
        lifecycleScope.launch {
            delay(PAYMENT_DEMO_DELAY_MS)
            if (!isFinishing) {
                startActivity(PurchaseSuccessActivity.createIntent(this@PaymentPendingActivity))
                finish()
            }
        }
    }

    private fun handleEffect(effect: CreditFlowUiEffect) {
        if (effect == CreditFlowUiEffect.NavigateBack) {
            finish()
        }
    }

    companion object {
        private const val PAYMENT_DEMO_DELAY_MS = 1500L

        fun createIntent(context: Context): Intent = Intent(context, PaymentPendingActivity::class.java)
    }
}
