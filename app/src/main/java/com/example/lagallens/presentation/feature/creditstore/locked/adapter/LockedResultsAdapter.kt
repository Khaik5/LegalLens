package com.example.lagallens.presentation.feature.creditstore.locked.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lagallens.R
import com.example.lagallens.databinding.ItemLockedResultBinding
import com.example.lagallens.domain.model.analysis.LockedAnalysisResult
import com.example.lagallens.presentation.common.utils.bindPressFeedback

class LockedResultsAdapter(
    private val onResultClicked: (LockedAnalysisResult) -> Unit
) : ListAdapter<LockedAnalysisResult, LockedResultsAdapter.ViewHolder>(Diff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemLockedResultBinding.inflate(LayoutInflater.from(parent.context), parent, false), onResultClicked)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class ViewHolder(
        private val binding: ItemLockedResultBinding,
        private val onResultClicked: (LockedAnalysisResult) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        init { binding.root.bindPressFeedback() }

        fun bind(result: LockedAnalysisResult) = with(binding) {
            tvLockedResultTitle.text = result.contractTitle
            tvLockedResultFile.text = result.fileName
            tvLockedResultFile.isVisible = result.fileName.isNotBlank()
            tvLockedResultCost.text = root.context.getString(R.string.locked_results_cost, result.unlockCost)
            tvLockedResultCost.isVisible = result.unlockCost > 0
            root.setOnClickListener { onResultClicked(result) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<LockedAnalysisResult>() {
        override fun areItemsTheSame(oldItem: LockedAnalysisResult, newItem: LockedAnalysisResult) = oldItem.resultId == newItem.resultId
        override fun areContentsTheSame(oldItem: LockedAnalysisResult, newItem: LockedAnalysisResult) = oldItem == newItem
    }
}
