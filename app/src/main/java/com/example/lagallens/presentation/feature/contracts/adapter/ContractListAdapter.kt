package com.example.lagallens.presentation.feature.contracts.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lagallens.databinding.ItemContractListBinding
import com.example.lagallens.presentation.common.utils.animatePressFeedback
import com.example.lagallens.presentation.feature.contracts.contract.ContractListItem

class ContractListAdapter(
    private val onContractClicked: (ContractListItem) -> Unit
) : ListAdapter<ContractListItem, ContractListAdapter.ContractViewHolder>(ContractDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContractViewHolder {
        val binding = ItemContractListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContractViewHolder(binding, onContractClicked)
    }

    override fun onBindViewHolder(holder: ContractViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ContractViewHolder(
        private val binding: ItemContractListBinding,
        private val onContractClicked: (ContractListItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        view.animate().cancel()
                        view.alpha = 1f
                        view.translationY = 0f
                        view.animatePressFeedback(true)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> view.animatePressFeedback(false)
                }
                false
            }
        }

        fun bind(item: ContractListItem) {
            val context = binding.root.context
            binding.ivContractIcon.setBackgroundResource(item.contractIconBackgroundRes)
            binding.ivContractIcon.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, item.contractIconTintRes)
            )
            binding.tvTitle.text = item.titleOverride ?: context.getString(item.titleRes)
            if (item.subtitleOverride == null) binding.tvSubtitle.setText(item.subtitleRes)
            else binding.tvSubtitle.text = item.subtitleOverride
            binding.tvStatus.setText(item.statusRes)
            binding.tvStatus.setBackgroundResource(item.statusBackgroundRes)
            binding.tvStatus.setTextColor(ContextCompat.getColor(context, item.statusTextColorRes))
            binding.ivRiskIcon.setImageResource(item.riskIconRes)
            binding.tvRisk.setText(item.riskRes)
            binding.tvRisk.setTextColor(ContextCompat.getColor(context, item.riskTextColorRes))
            binding.root.setOnClickListener { onContractClicked(item) }
        }
    }

    override fun onViewRecycled(holder: ContractViewHolder) {
        holder.itemView.animate().cancel()
        holder.itemView.alpha = 1f
        holder.itemView.translationY = 0f
        holder.itemView.scaleX = 1f
        holder.itemView.scaleY = 1f
        super.onViewRecycled(holder)
    }

    private object ContractDiffCallback : DiffUtil.ItemCallback<ContractListItem>() {
        override fun areItemsTheSame(oldItem: ContractListItem, newItem: ContractListItem): Boolean {
            return oldItem.titleRes == newItem.titleRes
        }

        override fun areContentsTheSame(oldItem: ContractListItem, newItem: ContractListItem): Boolean {
            return oldItem == newItem
        }
    }
}
