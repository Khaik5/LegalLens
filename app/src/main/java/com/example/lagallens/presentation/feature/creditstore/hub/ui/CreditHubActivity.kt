package com.example.lagallens.presentation.feature.creditstore.hub.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.R
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import com.example.lagallens.databinding.ActivityCreditHubBinding
import com.example.lagallens.presentation.feature.creditstore.flow.ui.CreditHistoryActivity
import com.example.lagallens.presentation.feature.creditstore.hub.contract.CreditHubUiEffect
import com.example.lagallens.presentation.feature.creditstore.hub.contract.CreditHubUiEvent
import com.example.lagallens.presentation.feature.creditstore.hub.contract.CreditHubUiState
import com.example.lagallens.presentation.feature.creditstore.hub.viewmodel.CreditHubViewModel
import com.example.lagallens.presentation.feature.creditstore.ui.CreditStoreActivity
import com.example.lagallens.presentation.feature.creditstore.unlock.ui.UnlockConfirmationActivity
import com.example.lagallens.LegalLensApplication
import kotlinx.coroutines.launch

class CreditHubActivity : AppCompatActivity() {
    private val binding by lazy { ActivityCreditHubBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<CreditHubViewModel> { CreditHubViewModel.Factory((application as LegalLensApplication).creditRepository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        bindView()
        collectViewModel()
        bindInsets()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                viewModel.onEvent(CreditHubUiEvent.BackClicked)
            }
        })
    }

    private fun bindView() = with(binding) {
        listOf(creditHubBuy, creditHubHistory, creditHubUnlock).forEach { it.bindPressFeedback() }
        creditHubBack.setOnClickListener {
            viewModel.onEvent(CreditHubUiEvent.BackClicked)
        }
        creditHubBuy.setOnClickListener {
            viewModel.onEvent(CreditHubUiEvent.BuyCreditsClicked)
        }
        creditHubHistory.setOnClickListener {
            viewModel.onEvent(CreditHubUiEvent.HistoryClicked)
        }
        creditHubUnlock.setOnClickListener {
            viewModel.onEvent(CreditHubUiEvent.UnlockResultsClicked)
        }
    }

    private fun collectViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.uiEffect.collect(::handleEffect) }
            }
        }
    }

    private fun bindInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.creditHubRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun render(state: CreditHubUiState) {
        binding.creditHubBalance.text = if (state.isBackendUnavailable) getString(R.string.credit_backend_not_configured)
        else getString(R.string.credit_hub_balance, state.balanceCredits)
    }

    private fun handleEffect(effect: CreditHubUiEffect) {
        when (effect) {
            CreditHubUiEffect.ShowBackendUnavailable -> Toast.makeText(this, R.string.credit_backend_not_configured, Toast.LENGTH_SHORT).show()
            CreditHubUiEffect.NavigateBack -> finish()
            CreditHubUiEffect.NavigateToStore -> startActivity(CreditStoreActivity.createIntent(this))
            CreditHubUiEffect.NavigateToHistory -> startActivity(CreditHistoryActivity.createIntent(this))
            CreditHubUiEffect.NavigateToUnlockConfirmation -> {
                startActivity(
                    UnlockConfirmationActivity.createIntent(
                        context = this,
                        analysisResultId = "KQ-1042",
                        availableCredits = viewModel.uiState.value.balanceCredits,
                        unlockCost = 2
                    )
                )
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent = Intent(context, CreditHubActivity::class.java)
    }
}
