package com.example.lagallens.presentation.feature.contracts.detail.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lagallens.databinding.ItemContractDetailAssistantBinding
import com.example.lagallens.presentation.feature.contracts.detail.contract.ContractAssistantAction

class ContractAssistantAdapter(
    private val onActionClicked: (ContractAssistantAction) -> Unit
) : ListAdapter<ContractAssistantAction, ContractAssistantAdapter.ContractAssistantViewHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContractAssistantViewHolder {
        val binding = ItemContractDetailAssistantBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContractAssistantViewHolder(binding, onActionClicked)
    }

    override fun onBindViewHolder(holder: ContractAssistantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ContractAssistantViewHolder(
        private val binding: ItemContractDetailAssistantBinding,
        private val onActionClicked: (ContractAssistantAction) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ContractAssistantAction) {
            binding.ivIcon.setImageResource(item.iconRes)
            binding.tvTitle.setText(item.titleRes)
            binding.root.setOnClickListener { onActionClicked(item) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ContractAssistantAction>() {
        override fun areItemsTheSame(oldItem: ContractAssistantAction, newItem: ContractAssistantAction) = oldItem == newItem
        override fun areContentsTheSame(oldItem: ContractAssistantAction, newItem: ContractAssistantAction) = oldItem == newItem
    }
}
