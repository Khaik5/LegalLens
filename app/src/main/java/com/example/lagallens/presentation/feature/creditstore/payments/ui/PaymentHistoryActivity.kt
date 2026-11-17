package com.example.lagallens.presentation.feature.creditstore.payments.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityPaymentHistoryBinding
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import com.example.lagallens.presentation.feature.creditstore.payments.adapter.PaymentHistoryAdapter
import com.example.lagallens.presentation.feature.creditstore.payments.contract.PaymentHistoryUiEffect
import com.example.lagallens.presentation.feature.creditstore.payments.contract.PaymentHistoryUiEvent
import com.example.lagallens.presentation.feature.creditstore.payments.contract.PaymentHistoryUiState
import com.example.lagallens.presentation.feature.creditstore.payments.viewmodel.PaymentHistoryViewModel
import kotlinx.coroutines.launch

/** The user's Credit purchases, including pending, failed and cancelled ones that Credit History does not list. */
class PaymentHistoryActivity : AppCompatActivity() {
    private val binding by lazy { ActivityPaymentHistoryBinding.inflate(layoutInflater) }
    private val owner by lazy { intent.getStringExtra(EXTRA_OWNER).orEmpty() }
    private val adapter = PaymentHistoryAdapter()
    private val viewModel by viewModels<PaymentHistoryViewModel> {
        PaymentHistoryViewModel.Factory((application as LegalLensApplication).paymentRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (owner.isBlank() || (application as LegalLensApplication).paymentRepository.activeSessionKey() != owner) {
            finish()
            return
        }
        enableEdgeToEdge()
        setContentView(binding.root)
        bindView()
        ViewCompat.setOnApplyWindowInsetsListener(binding.paymentHistoryRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = viewModel.onEvent(PaymentHistoryUiEvent.BackClicked)
        })
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.uiEffect.collect(::handleEffect) }
            }
        }
        viewModel.onEvent(PaymentHistoryUiEvent.ScreenOpened)
    }

    private fun bindView() = with(binding) {
        listOf(paymentHistoryBack, btnPaymentHistoryRetry).forEach { it.bindPressFeedback() }
        rvPaymentHistory.layoutManager = LinearLayoutManager(this@PaymentHistoryActivity)
        rvPaymentHistory.adapter = adapter
        paymentHistoryBack.setOnClickListener { viewModel.onEvent(PaymentHistoryUiEvent.BackClicked) }
        btnPaymentHistoryRetry.setOnClickListener { viewModel.onEvent(PaymentHistoryUiEvent.RetryClicked) }
    }

    private fun render(state: PaymentHistoryUiState) = with(binding) {
        val ownerSession = viewModel.isOwnerSession()
        val messageRes = when {
            !ownerSession -> R.string.payment_history_error
            state.errorMessageRes != null -> state.errorMessageRes
            !state.isLoading && state.entries.isEmpty() -> R.string.payment_history_empty
            else -> null
        }
        pbPaymentHistory.isVisible = state.isLoading && ownerSession
        rvPaymentHistory.isVisible = ownerSession && state.entries.isNotEmpty()
        groupPaymentHistoryMessage.isVisible = messageRes != null
        if (messageRes != null) tvPaymentHistoryMessage.setText(messageRes)
        btnPaymentHistoryRetry.visibility = if (state.errorMessageRes != null) View.VISIBLE else View.GONE
        adapter.submitList(if (ownerSession) state.entries else emptyList())
    }

    private fun handleEffect(effect: PaymentHistoryUiEffect) {
        when (effect) {
            PaymentHistoryUiEffect.NavigateBack -> finish()
        }
    }

    companion object {
        private const val EXTRA_OWNER = "payment_history_owner"
        fun createIntent(context: Context): Intent = Intent(context, PaymentHistoryActivity::class.java)
            .putExtra(EXTRA_OWNER, (context.applicationContext as LegalLensApplication).paymentRepository.activeSessionKey())
    }
}
