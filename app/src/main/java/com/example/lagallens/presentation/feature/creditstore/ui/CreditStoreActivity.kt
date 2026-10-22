package com.example.lagallens.presentation.feature.creditstore.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityCreditStoreBinding
import com.example.lagallens.presentation.feature.creditstore.adapter.CreditPackageAdapter
import com.example.lagallens.presentation.feature.creditstore.contract.CreditStoreUiEffect
import com.example.lagallens.presentation.feature.creditstore.contract.CreditStoreUiEvent
import com.example.lagallens.presentation.feature.creditstore.contract.CreditStoreUiState
import com.example.lagallens.presentation.feature.creditstore.flow.ui.PurchaseConfirmationActivity
import com.example.lagallens.presentation.feature.creditstore.flow.ui.CreditHistoryActivity
import com.example.lagallens.presentation.feature.creditstore.viewmodel.CreditStoreViewModel
import kotlinx.coroutines.launch

class CreditStoreActivity : AppCompatActivity() {
    private val binding by lazy { ActivityCreditStoreBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<CreditStoreViewModel>()
    private val creditPackageAdapter = CreditPackageAdapter { creditPackage ->
        viewModel.onEvent(CreditStoreUiEvent.PackageClicked(creditPackage.id))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        bindView()
        collectViewModel()
        ViewCompat.setOnApplyWindowInsetsListener(binding.creditStoreRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun bindView() = with(binding) {
        creditStoreBack.setOnClickListener {
            viewModel.onEvent(CreditStoreUiEvent.BackClicked)
        }
        creditStoreTopUpAction.setOnClickListener {
            viewModel.onEvent(CreditStoreUiEvent.TopUpClicked)
        }
        creditStoreViewAllHistory.setOnClickListener {
            viewModel.onEvent(CreditStoreUiEvent.ViewHistoryClicked)
        }
        creditStorePackages.apply {
            layoutManager = LinearLayoutManager(this@CreditStoreActivity)
            adapter = creditPackageAdapter
            isNestedScrollingEnabled = false
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

    private fun render(state: CreditStoreUiState) = with(binding) {
        creditStoreBalance.text = getString(R.string.credit_store_balance_credits, state.balanceCredits)
        creditPackageAdapter.submitList(state.packages)
    }

    private fun handleEffect(effect: CreditStoreUiEffect) {
        when (effect) {
            CreditStoreUiEffect.NavigateBack -> finish()
            CreditStoreUiEffect.NavigateToPurchaseConfirmation -> {
                startActivity(PurchaseConfirmationActivity.createIntent(this))
            }
            CreditStoreUiEffect.NavigateToCreditHistory -> {
                startActivity(CreditHistoryActivity.createIntent(this))
            }
        }
    }

    override fun onDestroy() {
        binding.creditStorePackages.adapter = null
        super.onDestroy()
    }

    companion object {
        fun createIntent(context: Context): Intent = Intent(context, CreditStoreActivity::class.java)
    }
}
