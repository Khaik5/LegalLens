package com.example.lagallens.presentation.feature.creditstore.payments.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lagallens.R
import com.example.lagallens.databinding.ItemPaymentHistoryBinding
import com.example.lagallens.domain.model.payment.PaymentHistoryEntry
import com.example.lagallens.domain.model.payment.PaymentStatus
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PaymentHistoryAdapter : ListAdapter<PaymentHistoryEntry, PaymentHistoryAdapter.ViewHolder>(Diff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemPaymentHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class ViewHolder(private val binding: ItemPaymentHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(entry: PaymentHistoryEntry) {
            val context = binding.root.context
            val locale = Locale.getDefault()
            binding.tvPaymentCredits.text = context.getString(R.string.payment_history_credits, entry.credits)
            binding.tvPaymentAmount.text = context.getString(R.string.payment_history_amount, NumberFormat.getIntegerInstance(locale).format(entry.amountVnd))
            binding.tvPaymentDate.text = if (entry.createdAtEpochMillis > 0) {
                SimpleDateFormat(DATE_PATTERN, locale).format(Date(entry.createdAtEpochMillis))
            } else {
                ""
            }
            binding.tvPaymentStatus.setText(statusLabel(entry.status))
            binding.tvPaymentStatus.setTextColor(ContextCompat.getColor(context, statusColor(entry.status)))
        }

        @StringRes private fun statusLabel(status: PaymentStatus) = when (status) {
            PaymentStatus.PENDING -> R.string.payment_history_status_pending
            PaymentStatus.SUCCESS -> R.string.payment_history_status_success
            PaymentStatus.FAILED -> R.string.payment_history_status_failed
            PaymentStatus.CANCELLED -> R.string.payment_history_status_cancelled
        }

        @ColorRes private fun statusColor(status: PaymentStatus) = when (status) {
            PaymentStatus.PENDING -> R.color.credit_warning
            PaymentStatus.SUCCESS -> R.color.legal_lens_auth_accent
            PaymentStatus.FAILED -> R.color.contract_risk_high
            PaymentStatus.CANCELLED -> R.color.legal_lens_secondary_text
        }
    }

    private object Diff : DiffUtil.ItemCallback<PaymentHistoryEntry>() {
        override fun areItemsTheSame(oldItem: PaymentHistoryEntry, newItem: PaymentHistoryEntry) = oldItem.paymentId == newItem.paymentId
        override fun areContentsTheSame(oldItem: PaymentHistoryEntry, newItem: PaymentHistoryEntry) = oldItem == newItem
    }

    private companion object {
        const val DATE_PATTERN = "dd/MM/yyyy HH:mm"
    }
}
