package com.example.lagallens.presentation.feature.creditstore.flow.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.lagallens.domain.model.payment.BankTransfer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import com.example.lagallens.R
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.databinding.ActivityPaymentPendingBinding
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEffect
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEvent
import com.example.lagallens.presentation.feature.creditstore.flow.utils.paymentFailureMessage
import kotlinx.coroutines.CancellationException

class PaymentPendingActivity : CreditFlowActivity() {
    private val binding by lazy { ActivityPaymentPendingBinding.inflate(layoutInflater) }
    private lateinit var selection: CreditPurchaseSelection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selection = CreditPurchaseSelection.fromIntent(intent) ?: run { finish(); return }
        enableEdgeToEdge()
        setContentView(binding.root)
        binding.pendingPackage.text = selection.creditPackage.title
        binding.pendingPrice.text = selection.priceText
        binding.pendingTransactionId.text = selection.transactionReference
        binding.pendingSubmittedAt.text = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT).format(java.util.Date())
        val creditRepository = (application as LegalLensApplication).creditRepository
        binding.pendingBalance.text = if (creditRepository.isConfigured) getString(R.string.credit_store_balance_credits, creditRepository.balance.value)
        else getString(R.string.credit_backend_not_configured)
        binding.pendingCredits.text = getString(R.string.credit_purchase_added, selection.creditPackage.credits)
        binding.pendingCreditsNote.text = getString(R.string.credit_purchase_pending_note, selection.creditPackage.credits)
        viewModel.onEvent(CreditFlowUiEvent.StartPayment(selection.purchaseId, selection.creditPackage.id))
        binding.paymentPendingBack.setOnClickListener {
            viewModel.onEvent(CreditFlowUiEvent.BackClicked)
        }
        applySystemBarInsets(binding.paymentPendingRoot)
        bindSystemBack()
        collectEffects(::handleEffect)
        collectBankTransfer()
    }

    private fun collectBankTransfer() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    binding.pendingQrUnavailable.isVisible = state.isBankTransferUnavailable
                    val transfer = state.bankTransfer
                    binding.pendingQrCard.isVisible = transfer != null
                    if (transfer != null) showBankTransfer(transfer)
                }
            }
        }
    }

    private suspend fun showBankTransfer(transfer: BankTransfer) {
        Glide.with(binding.pendingQrImage).load(transfer.qrUrl).into(binding.pendingQrImage)
        binding.pendingQrCode.text = transfer.paymentCode
        binding.pendingQrAmount.text = getString(R.string.credit_flow_qr_amount_value, NumberFormat.getInstance(Locale("vi", "VN")).format(transfer.amountVnd))
        binding.pendingQrCopy.setOnClickListener {
            val clipboard = getSystemService(ClipboardManager::class.java)
            clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.credit_flow_qr_transfer_content), transfer.paymentCode))
            Toast.makeText(this, R.string.credit_flow_qr_copied, Toast.LENGTH_SHORT).show()
        }
        val expiresAt = transfer.expiresAtEpochMillis
        binding.pendingQrExpiryRow.isVisible = expiresAt != null
        if (expiresAt == null) return
        // The timer is cancelled with the collector, so it does not run in the background.
        kotlinx.coroutines.coroutineScope {
            while (true) {
                val remainingSeconds = ((expiresAt - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
                binding.pendingQrCountdown.text = if (remainingSeconds == 0L) getString(R.string.credit_flow_qr_expired)
                else String.format(Locale.US, "%02d:%02d", remainingSeconds / 60, remainingSeconds % 60)
                if (remainingSeconds == 0L) break
                delay(1_000)
            }
        }
    }

    private fun handleEffect(effect: CreditFlowUiEffect) {
        when (effect) {
            CreditFlowUiEffect.NavigateBack -> finish()
            is CreditFlowUiEffect.PaymentSucceeded -> {
                startActivity(PurchaseSuccessActivity.createIntent(this, selection))
                finish()
            }
            CreditFlowUiEffect.PaymentFailed -> {
                lifecycleScope.launch {
                    val payment = try {
                        (application as LegalLensApplication).paymentRepository.get(selection.purchaseId)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        null
                    }
                    val message = paymentFailureMessage(
                        payment?.status, viewModel.uiState.value.bankTransfer?.expiresAtEpochMillis, System.currentTimeMillis()
                    )
                    Toast.makeText(this@PaymentPendingActivity, message, Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            else -> Unit
        }
    }

    companion object {
        internal fun createIntent(context: Context, selection: CreditPurchaseSelection): Intent = selection.putInto(Intent(context, PaymentPendingActivity::class.java))
    }
}
