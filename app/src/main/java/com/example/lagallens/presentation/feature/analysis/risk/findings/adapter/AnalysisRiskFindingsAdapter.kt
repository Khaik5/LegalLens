package com.example.lagallens.presentation.feature.analysis.risk.findings.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lagallens.databinding.ItemAnalysisRiskFindingBinding
import com.example.lagallens.presentation.feature.analysis.risk.findings.model.AnalysisRiskFinding

class AnalysisRiskFindingsAdapter(
    private val onFindingClicked: (String) -> Unit
) : ListAdapter<AnalysisRiskFinding, AnalysisRiskFindingsAdapter.AnalysisRiskFindingViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnalysisRiskFindingViewHolder {
        val binding = ItemAnalysisRiskFindingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AnalysisRiskFindingViewHolder(binding, onFindingClicked)
    }

    override fun onBindViewHolder(holder: AnalysisRiskFindingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AnalysisRiskFindingViewHolder(
        private val binding: ItemAnalysisRiskFindingBinding,
        private val onFindingClicked: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AnalysisRiskFinding) = with(binding) {
            analysisRiskFindingSeverity.setText(item.severity.labelRes)
            analysisRiskFindingSeverity.setBackgroundResource(item.severity.badgeBackgroundRes)
            analysisRiskFindingSeverity.setTextColor(
                itemView.context.getColor(item.severity.labelColorRes)
            )
            analysisRiskFindingClause.setText(item.clauseRes)
            analysisRiskFindingTitle.setText(item.titleRes)
            analysisRiskFindingDescription.setText(item.descriptionRes)
            analysisRiskFindingConsequence.setText(item.consequenceRes)
            root.setOnClickListener { onFindingClicked(item.id) }
            analysisRiskFindingSolution.setOnClickListener { onFindingClicked(item.id) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<AnalysisRiskFinding>() {
        override fun areItemsTheSame(
            oldItem: AnalysisRiskFinding,
            newItem: AnalysisRiskFinding
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: AnalysisRiskFinding,
            newItem: AnalysisRiskFinding
        ): Boolean = oldItem == newItem
    }
}
