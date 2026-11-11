package com.example.lagallens.presentation.feature.creditstore.flow.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import com.example.lagallens.R
import com.example.lagallens.domain.model.credit.CreditTransactionKind
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.view.ViewGroup
import android.graphics.Color
import android.util.TypedValue
import com.example.lagallens.LegalLensApplication
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.lagallens.databinding.ActivityCreditHistoryBinding
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEffect
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEvent
import com.example.lagallens.presentation.feature.creditstore.ui.CreditStoreActivity

class CreditHistoryActivity : CreditFlowActivity() {
    private val binding by lazy { ActivityCreditHistoryBinding.inflate(layoutInflater) }
    private val owner by lazy { intent.getStringExtra(EXTRA_OWNER).orEmpty() }
    private fun isOwner(): Boolean = owner.isNotBlank() && (application as LegalLensApplication).creditRepository.activeSessionKey() == owner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.alpha = 0f
        if (!isOwner()) { finish(); return }
        enableEdgeToEdge()
        setContentView(binding.root)
        val repository = (application as LegalLensApplication).creditRepository
        binding.creditHistoryBalance.setText(R.string.credit_backend_not_configured)
        renderTransactions(emptyList(), loading = true)
        lifecycleScope.launch {
            repository.observeSessionKeys().collect { if (!isOwner()) { window.decorView.alpha = 0f; finish() } }
        }
        if (!repository.isConfigured) {
            binding.creditHistoryBalance.setText(R.string.credit_backend_not_configured)
            renderTransactions(emptyList(), backendUnavailable = true)
        } else {
            lifecycleScope.launch {
                repository.balance.collect { if (isOwner()) binding.creditHistoryBalance.text = getString(R.string.credit_store_balance_credits, it) else { window.decorView.alpha = 0f; finish() } }
            }
            lifecycleScope.launch {
                val transactions = runCatching {
                    repository.refresh()
                    repository.getTransactions()
                }.getOrNull()
                if (!isOwner() || isFinishing || isDestroyed) return@launch
                if (transactions == null) renderTransactions(emptyList(), failed = true)
                else renderTransactions(transactions)
            }
        }
        listOf(binding.creditHistoryBack, binding.creditHistoryBuyMore).forEach { it.bindPressFeedback() }
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

    override fun onResume() {
        super.onResume()
        if (!isOwner()) { window.decorView.alpha = 0f; finish() } else window.decorView.alpha = 1f
    }

    override fun onPause() { window.decorView.alpha = 0f; super.onPause() }

    private fun renderTransactions(
        transactions: List<com.example.lagallens.domain.model.credit.CreditTransaction>,
        backendUnavailable: Boolean = false,
        loading: Boolean = false,
        failed: Boolean = false
    ) {
        val scrollView = (0 until binding.creditHistoryRoot.childCount)
            .mapNotNull { binding.creditHistoryRoot.getChildAt(it) as? ScrollView }.firstOrNull() ?: return
        val content = scrollView.getChildAt(0) as? LinearLayout ?: return
        val header = content.getChildAt(3) as? LinearLayout
        (header?.getChildAt(0) as? TextView)?.text = getString(R.string.credit_flow_history_count, transactions.size)
        (header?.getChildAt(1) as? TextView)?.visibility = android.view.View.GONE
        while (content.childCount > 4) content.removeViewAt(4)
        val rows = transactions.sortedByDescending { it.createdAtEpochMillis }
        if (loading) {
            content.addView(row(getString(R.string.credit_history_loading)))
            return
        }
        if (failed) {
            content.addView(row(getString(R.string.credit_history_error)))
            return
        }
        if (backendUnavailable) {
            content.addView(row(getString(R.string.credit_history_backend_not_configured)))
            return
        }
        if (rows.isEmpty()) {
            content.addView(row(getString(R.string.credit_history_empty)))
            return
        }
        rows.forEach { transaction ->
            val sign = if (transaction.creditDelta > 0) "+" else "−"
            val detail = transaction.kind?.let { kind ->
                getString(when (kind) {
                    CreditTransactionKind.PURCHASE -> R.string.credit_history_purchase
                    CreditTransactionKind.USAGE -> R.string.credit_history_analysis_unlock
                    CreditTransactionKind.REFUND -> R.string.credit_history_refund
                    CreditTransactionKind.BONUS -> R.string.credit_history_bonus
                })
            } ?: if (transaction.amountVnd > 0) getString(
                R.string.credit_history_purchase_detail, transaction.packageId.orEmpty(), transaction.amountVnd
            ) else getString(if (transaction.creditDelta < 0) R.string.credit_history_analysis_unlock else R.string.credit_history_credit_adjustment)
            val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(transaction.createdAtEpochMillis))
            content.addView(row(getString(R.string.credit_history_row, sign, kotlin.math.abs(transaction.creditDelta), getString(when (transaction.status.uppercase(Locale.ROOT)) {
                "SUCCESS", "COMPLETED", "APPLIED" -> R.string.credit_history_completed
                "PENDING" -> R.string.credit_history_pending
                "FAILED" -> R.string.credit_history_failed
                else -> R.string.credit_history_recorded
            }), "$date · $detail")))
        }
    }

    private fun row(text: String): TextView = TextView(this).apply {
        setTextColor(Color.DKGRAY)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        setPadding(16.dp(), 14.dp(), 16.dp(), 14.dp())
        background = androidx.core.content.ContextCompat.getDrawable(this@CreditHistoryActivity, R.drawable.bg_contract_card)
        this.text = text
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = 12.dp() }
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    private fun handleEffect(effect: CreditFlowUiEffect) {
        when (effect) {
            CreditFlowUiEffect.NavigateBack -> finish()
            CreditFlowUiEffect.NavigateToCreditStore -> startActivity(CreditStoreActivity.createIntent(this))
            else -> Unit
        }
    }

    companion object {
        private const val EXTRA_OWNER = "credit_history_owner"
        fun createIntent(context: Context): Intent = Intent(context, CreditHistoryActivity::class.java)
            .putExtra(EXTRA_OWNER, (context.applicationContext as LegalLensApplication).creditRepository.activeSessionKey())
    }
}
