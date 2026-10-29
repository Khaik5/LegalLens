package com.example.lagallens.presentation.feature.compare.select.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.lagallens.R
import com.example.lagallens.databinding.ItemCompareContractBinding
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import com.example.lagallens.presentation.feature.compare.select.contract.CompareContract
import com.example.lagallens.presentation.feature.compare.select.contract.CompareSelectUiState

class CompareContractAdapter(private val onClick: (String) -> Unit) : RecyclerView.Adapter<CompareContractAdapter.Holder>() {
    private var state = CompareSelectUiState()
    fun submitState(value: CompareSelectUiState) { state = value; notifyDataSetChanged() }
    override fun getItemCount() = state.contracts.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(ItemCompareContractBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(state.contracts[position], state, onClick)

    class Holder(private val binding: ItemCompareContractBinding) : RecyclerView.ViewHolder(binding.root) {
        init { binding.compareItemRoot.bindPressFeedback() }
        fun bind(item: CompareContract, state: CompareSelectUiState, onClick: (String) -> Unit) = with(binding) {
            val selected = item.id in state.selectedIds
            val atLimit = state.selectedIds.size == 2 && !selected
            compareItemTitle.text = item.title
            compareItemSubtitle.text = if (atLimit) root.context.getString(R.string.compare_not_selectable) else item.subtitle
            compareItemCheck.setImageResource(if (selected) R.drawable.bg_contract_filter_checkbox_checked else R.drawable.bg_contract_filter_checkbox_unchecked)
            compareItemRoot.setBackgroundResource(if (selected) R.drawable.bg_compare_selected else R.drawable.bg_contract_search_result)
            compareItemRoot.alpha = if (atLimit) 0.6f else 1f
            compareItemRoot.isEnabled = !atLimit
            compareItemStatus.isVisible = selected
            compareItemStatus.setText(if (selected && item.id == state.initialContractId && state.selectedIds.size == 1) R.string.compare_current_selected else R.string.compare_selected)
            compareItemRoot.setOnClickListener { onClick(item.id) }
        }
    }
}
