package com.example.lagallens.presentation.feature.contracts.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lagallens.databinding.ItemContractListBinding
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
        fun bind(item: ContractListItem) {
            val context = binding.root.context
            binding.ivContractIcon.setBackgroundResource(item.contractIconBackgroundRes)
            binding.ivContractIcon.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, item.contractIconTintRes)
            )
            binding.tvTitle.setText(item.titleRes)
            binding.tvSubtitle.setText(item.subtitleRes)
            binding.tvStatus.setText(item.statusRes)
            binding.tvStatus.setBackgroundResource(item.statusBackgroundRes)
            binding.tvStatus.setTextColor(ContextCompat.getColor(context, item.statusTextColorRes))
            binding.ivRiskIcon.setImageResource(item.riskIconRes)
            binding.tvRisk.setText(item.riskRes)
            binding.tvRisk.setTextColor(ContextCompat.getColor(context, item.riskTextColorRes))
            binding.root.setOnClickListener { onContractClicked(item) }
        }
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
