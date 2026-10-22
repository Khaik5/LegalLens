package com.example.lagallens.presentation.feature.creditstore.flow.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import com.example.lagallens.databinding.ActivityUnlockedAnalysisResultBinding
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEffect
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEvent

class UnlockedAnalysisResultActivity : CreditFlowActivity() {
    private val binding by lazy { ActivityUnlockedAnalysisResultBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        binding.unlockedAnalysisBack.setOnClickListener {
            viewModel.onEvent(CreditFlowUiEvent.BackClicked)
        }
        binding.unlockedAnalysisHistory.setOnClickListener {
            viewModel.onEvent(CreditFlowUiEvent.OpenAnalysisHistoryClicked)
        }
        applySystemBarInsets(binding.unlockedAnalysisRoot)
        bindSystemBack()
        collectEffects(::handleEffect)
    }

    private fun handleEffect(effect: CreditFlowUiEffect) {
        when (effect) {
            CreditFlowUiEffect.NavigateBack -> finish()
            CreditFlowUiEffect.ReturnToAnalysisHistory -> {
                setResult(RESULT_OK)
                finish()
            }

            else -> Unit
        }
    }

    companion object {
        fun createIntent(context: Context): Intent = Intent(context, UnlockedAnalysisResultActivity::class.java)
    }
}
