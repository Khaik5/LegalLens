package com.example.lagallens.presentation.feature.analysis.keyfacts.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lagallens.databinding.ItemAnalysisKeyFactBinding
import com.example.lagallens.presentation.feature.analysis.keyfacts.contract.AnalysisClauseSource
import com.example.lagallens.presentation.feature.analysis.keyfacts.contract.AnalysisKeyFact

class AnalysisKeyFactsAdapter(
    private val onSourceClicked: (AnalysisClauseSource) -> Unit
) : ListAdapter<AnalysisKeyFact, AnalysisKeyFactsAdapter.ViewHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        ItemAnalysisKeyFactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onSourceClicked)
    }

    class ViewHolder(
        private val binding: ItemAnalysisKeyFactBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            item: AnalysisKeyFact,
            onSourceClicked: (AnalysisClauseSource) -> Unit
        ) = with(binding) {
            analysisKeyFactLabel.setText(item.labelRes)
            analysisKeyFactReference.setText(item.referenceRes)
            analysisKeyFactValue.setText(item.valueRes)
            analysisKeyFactDescription.setText(item.descriptionRes)
            analysisKeyFactSource.setOnClickListener { item.source?.let(onSourceClicked) }
            analysisKeyFactSource.isClickable = item.source != null
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<AnalysisKeyFact>() {
        override fun areItemsTheSame(oldItem: AnalysisKeyFact, newItem: AnalysisKeyFact): Boolean =
            oldItem.labelRes == newItem.labelRes

        override fun areContentsTheSame(oldItem: AnalysisKeyFact, newItem: AnalysisKeyFact): Boolean =
            oldItem == newItem
    }
}
