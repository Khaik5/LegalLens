package com.example.lagallens.presentation.feature.analysis.summary.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lagallens.databinding.ItemAnalysisSummarySectionBinding
import com.example.lagallens.presentation.feature.analysis.summary.contract.AnalysisSummarySection

class AnalysisSummaryAdapter : ListAdapter<AnalysisSummarySection, AnalysisSummaryAdapter.ViewHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        ItemAnalysisSummarySectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemAnalysisSummarySectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AnalysisSummarySection) = with(binding) {
            analysisSummarySectionTitle.setText(item.titleRes)
            analysisSummarySectionDescription.setText(item.descriptionRes)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<AnalysisSummarySection>() {
        override fun areItemsTheSame(oldItem: AnalysisSummarySection, newItem: AnalysisSummarySection): Boolean =
            oldItem.titleRes == newItem.titleRes

        override fun areContentsTheSame(oldItem: AnalysisSummarySection, newItem: AnalysisSummarySection): Boolean =
            oldItem == newItem
    }
}
