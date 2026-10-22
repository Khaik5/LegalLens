package com.example.lagallens.presentation.feature.creditstore.flow.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.databinding.ActivityPurchaseConfirmationBinding
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEffect
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEvent
import kotlinx.coroutines.launch

class PurchaseConfirmationActivity : CreditFlowActivity() {
    private val binding by lazy { ActivityPurchaseConfirmationBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
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
                    binding.purchaseConfirmationPay.isEnabled = !state.isPaymentRequesting
                }
            }
        }
    }

    private fun handleEffect(effect: CreditFlowUiEffect) {
        when (effect) {
            CreditFlowUiEffect.NavigateBack -> finish()
            CreditFlowUiEffect.PaymentRequested -> {
                startActivity(PaymentPendingActivity.createIntent(this))
                finish()
            }

            else -> Unit
        }
    }

    companion object {
        fun createIntent(context: Context): Intent = Intent(context, PurchaseConfirmationActivity::class.java)
    }
}
