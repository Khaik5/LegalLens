package com.example.lagallens.presentation.feature.creditstore.flow.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import com.example.lagallens.R
import com.example.lagallens.domain.model.credit.CreditTransactionKind
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import android.widget.LinearLayout
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
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditHistoryFilter
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEffect
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEvent
import com.example.lagallens.presentation.feature.creditstore.ui.CreditStoreActivity
import com.example.lagallens.presentation.feature.creditstore.utils.showCreditBalance

class CreditHistoryActivity : CreditFlowActivity() {
    private val binding by lazy { ActivityCreditHistoryBinding.inflate(layoutInflater) }
    private val owner by lazy { intent.getStringExtra(EXTRA_OWNER).orEmpty() }
    private var selectedFilter = CreditHistoryFilter.ALL
    private var shownTransactions = emptyList<com.example.lagallens.domain.model.credit.CreditTransaction>()
    private var shownBackendUnavailable = false
    private var shownLoading = false
    private var shownFailed = false
    /** The repository holds 0 until the server answers, so the balance is shown only after a successful load. */
    private var balanceLoaded = false

    private fun isOwner(): Boolean = owner.isNotBlank() && (application as LegalLensApplication).creditRepository.activeSessionKey() == owner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.alpha = 0f
        if (!isOwner()) { finish(); return }
        enableEdgeToEdge()
        setContentView(binding.root)
        val repository = (application as LegalLensApplication).creditRepository
        binding.creditHistoryBalance.showCreditBalance(getString(R.string.credit_balance_loading), isMessage = true, balanceTextSize = R.dimen.sp_24)
        renderTransactions(emptyList(), loading = true)
        lifecycleScope.launch {
            repository.observeSessionKeys().collect { if (!isOwner()) { window.decorView.alpha = 0f; finish() } }
        }
        if (!repository.isConfigured) {
            binding.creditHistoryBalance.showCreditBalance(getString(R.string.credit_backend_not_configured), isMessage = true, balanceTextSize = R.dimen.sp_24)
            renderTransactions(emptyList(), backendUnavailable = true)
        } else {
            lifecycleScope.launch {
                repository.balance.collect {
                    if (!isOwner()) { window.decorView.alpha = 0f; finish() } else if (balanceLoaded) showBalance(it)
                }
            }
            loadHistory()
        }
        selectedFilter = savedInstanceState?.getString(STATE_FILTER)?.let { CreditHistoryFilter.valueOf(it) } ?: CreditHistoryFilter.ALL
        styleTabs()
        mapOf(
            binding.tabCreditHistoryAll to CreditHistoryFilter.ALL,
            binding.tabCreditHistoryPurchase to CreditHistoryFilter.PURCHASE,
            binding.tabCreditHistoryUsage to CreditHistoryFilter.USAGE
        ).forEach { (tab, filter) ->
            tab.setOnClickListener {
                if (selectedFilter == filter) return@setOnClickListener
                selectedFilter = filter
                styleTabs()
                renderTransactions(shownTransactions, shownBackendUnavailable, shownLoading, shownFailed)
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

    private fun loadHistory() {
        val repository = (application as LegalLensApplication).creditRepository
        renderTransactions(emptyList(), loading = true)
        lifecycleScope.launch {
            val transactions = try {
                repository.refresh()
                repository.getTransactions()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            if (!isOwner() || isFinishing || isDestroyed) return@launch
            balanceLoaded = transactions != null
            if (transactions == null) {
                binding.creditHistoryBalance.showCreditBalance(getString(R.string.credit_balance_load_failed), isMessage = true, balanceTextSize = R.dimen.sp_24)
                renderTransactions(emptyList(), failed = true)
            } else {
                showBalance(repository.balance.value)
                renderTransactions(transactions)
            }
        }
    }

    private fun showBalance(credits: Int) {
        binding.creditHistoryBalance.showCreditBalance(getString(R.string.credit_store_balance_credits, credits), isMessage = false, balanceTextSize = R.dimen.sp_24)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_FILTER, selectedFilter.name)
        super.onSaveInstanceState(outState)
    }

    private fun styleTabs() {
        mapOf(
            binding.tabCreditHistoryAll to CreditHistoryFilter.ALL,
            binding.tabCreditHistoryPurchase to CreditHistoryFilter.PURCHASE,
            binding.tabCreditHistoryUsage to CreditHistoryFilter.USAGE
        ).forEach { (tab, filter) ->
            val isSelected = filter == selectedFilter
            tab.background = if (isSelected) androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_credit_history_segment_selected) else null
            tab.setTextColor(androidx.core.content.ContextCompat.getColor(this, if (isSelected) R.color.legal_lens_auth_accent else R.color.legal_lens_secondary_text))
            tab.setTypeface(null, if (isSelected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isOwner()) { window.decorView.alpha = 0f; finish() } else window.decorView.alpha = 1f
    }

    override fun onPause() { window.decorView.alpha = 0f; super.onPause() }

    private fun renderTransactions(
        allTransactions: List<com.example.lagallens.domain.model.credit.CreditTransaction>,
        backendUnavailable: Boolean = false,
        loading: Boolean = false,
        failed: Boolean = false
    ) {
        shownTransactions = allTransactions
        shownBackendUnavailable = backendUnavailable
        shownLoading = loading
        shownFailed = failed
        val transactions = allTransactions.filter(selectedFilter::matches)
        binding.creditHistoryCount.text = getString(R.string.credit_flow_history_count, transactions.size)
        binding.creditHistoryPeriod.visibility = android.view.View.GONE
        val content = binding.creditHistoryTransactions
        content.removeAllViews()
        val rows = transactions.sortedByDescending { it.createdAtEpochMillis }
        if (loading) {
            content.addView(row(getString(R.string.credit_history_loading)))
            return
        }
        if (failed) {
            content.addView(row(getString(R.string.credit_history_error)).apply {
                isClickable = true
                isFocusable = true
                setOnClickListener { loadHistory() }
            })
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
        private const val STATE_FILTER = "credit_history_filter"
        fun createIntent(context: Context): Intent = Intent(context, CreditHistoryActivity::class.java)
            .putExtra(EXTRA_OWNER, (context.applicationContext as LegalLensApplication).creditRepository.activeSessionKey())
    }
}
