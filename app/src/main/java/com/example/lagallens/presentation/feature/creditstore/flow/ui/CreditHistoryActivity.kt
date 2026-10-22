package com.example.lagallens.presentation.feature.creditstore.flow.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import com.example.lagallens.databinding.ActivityCreditHistoryBinding
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEffect
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEvent
import com.example.lagallens.presentation.feature.creditstore.ui.CreditStoreActivity

class CreditHistoryActivity : CreditFlowActivity() {
    private val binding by lazy { ActivityCreditHistoryBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        binding.creditHistoryBack.setOnClickListener {
            viewModel.onEvent(CreditFlowUiEvent.BackClicked)
        }
        binding.creditHistoryBuyMore.setOnClickListener {
            viewModel.onEvent(CreditFlowUiEvent.OpenCreditStoreClicked)
        }
        applySystemBarInsets(binding.creditHistoryRoot)
        bindSystemBack()
        collectEffects(::handleEffect)
    }

    private fun handleEffect(effect: CreditFlowUiEffect) {
        when (effect) {
            CreditFlowUiEffect.NavigateBack -> finish()
            CreditFlowUiEffect.NavigateToCreditStore -> startActivity(CreditStoreActivity.createIntent(this))
            else -> Unit
        }
    }

    companion object {
        fun createIntent(context: Context): Intent = Intent(context, CreditHistoryActivity::class.java)
    }
}
