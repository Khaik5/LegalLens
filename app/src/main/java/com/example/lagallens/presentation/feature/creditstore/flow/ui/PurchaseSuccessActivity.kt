package com.example.lagallens.presentation.feature.creditstore.flow.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import com.example.lagallens.databinding.ActivityPurchaseSuccessBinding
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEffect
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEvent
import com.example.lagallens.presentation.feature.creditstore.unlock.ui.UnlockConfirmationActivity

class PurchaseSuccessActivity : CreditFlowActivity() {
    private val binding by lazy { ActivityPurchaseSuccessBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        binding.purchaseSuccessBack.setOnClickListener {
            viewModel.onEvent(CreditFlowUiEvent.BackClicked)
        }
        binding.purchaseSuccessReturnToUnlock.setOnClickListener {
            viewModel.onEvent(CreditFlowUiEvent.ReturnToUnlockClicked)
        }
        binding.purchaseSuccessHistory.setOnClickListener {
            viewModel.onEvent(CreditFlowUiEvent.OpenCreditHistoryClicked)
        }
        applySystemBarInsets(binding.purchaseSuccessRoot)
        bindSystemBack()
        collectEffects(::handleEffect)
    }

    private fun handleEffect(effect: CreditFlowUiEffect) {
        when (effect) {
            CreditFlowUiEffect.NavigateBack -> finish()
            CreditFlowUiEffect.NavigateToCreditHistory -> startActivity(CreditHistoryActivity.createIntent(this))
            CreditFlowUiEffect.ReturnToUnlockConfirmation -> {
                startActivity(
                    UnlockConfirmationActivity.createIntent(
                        context = this,
                        analysisResultId = "KQ-1042",
                        availableCredits = 16,
                        unlockCost = 2
                    )
                )
                finish()
            }

            else -> Unit
        }
    }

    companion object {
        fun createIntent(context: Context): Intent = Intent(context, PurchaseSuccessActivity::class.java)
    }
}
