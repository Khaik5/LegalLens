package com.example.lagallens.presentation.feature.contracts.search.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lagallens.databinding.ItemContractSearchResultBinding
import com.example.lagallens.presentation.feature.contracts.search.contract.ContractSearchResult

class ContractSearchAdapter(
    private val onResultClicked: (ContractSearchResult) -> Unit
) : ListAdapter<ContractSearchResult, ContractSearchAdapter.ContractSearchViewHolder>(ContractSearchDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContractSearchViewHolder {
        val binding = ItemContractSearchResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ContractSearchViewHolder(binding, onResultClicked)
    }

    override fun onBindViewHolder(holder: ContractSearchViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ContractSearchViewHolder(
        private val binding: ItemContractSearchResultBinding,
        private val onResultClicked: (ContractSearchResult) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ContractSearchResult) {
            binding.tvTitle.setText(item.titleRes)
            binding.tvSubtitle.setText(item.subtitleRes)
            binding.root.setOnClickListener { onResultClicked(item) }
        }
    }

    private object ContractSearchDiffCallback : DiffUtil.ItemCallback<ContractSearchResult>() {
        override fun areItemsTheSame(
            oldItem: ContractSearchResult,
            newItem: ContractSearchResult
        ): Boolean = oldItem.titleRes == newItem.titleRes

        override fun areContentsTheSame(
            oldItem: ContractSearchResult,
            newItem: ContractSearchResult
        ): Boolean = oldItem == newItem
    }
}
