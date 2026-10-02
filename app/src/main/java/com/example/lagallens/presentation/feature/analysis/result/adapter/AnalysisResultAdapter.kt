package com.example.lagallens.presentation.feature.analysis.result.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.lagallens.R
import com.example.lagallens.databinding.ItemAnalysisResultBinding
import com.example.lagallens.presentation.feature.analysis.result.contract.AnalysisResultDestination
import com.example.lagallens.presentation.feature.analysis.result.contract.AnalysisResultItem

class AnalysisResultAdapter(
    private val onItemClicked: (AnalysisResultDestination) -> Unit
) : ListAdapter<AnalysisResultItem, AnalysisResultAdapter.ViewHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        ItemAnalysisResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClicked)
    }

    class ViewHolder(
        private val binding: ItemAnalysisResultBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            item: AnalysisResultItem,
            onItemClicked: (AnalysisResultDestination) -> Unit
        ) = with(binding) {
            analysisResultItemIcon.setImageResource(item.iconRes)
            analysisResultItemIcon.setColorFilter(
                ContextCompat.getColor(
                    root.context,
                    if (item.isRisk) R.color.analysis_result_risk else R.color.ocr_saved_teal
                )
            )
            analysisResultItemIconContainer.setBackgroundResource(
                if (item.isRisk) R.drawable.bg_analysis_result_risk_icon else R.drawable.bg_analysis_result_icon
            )
            analysisResultItemTitle.setText(item.titleRes)
            analysisResultItemDescription.setText(item.descriptionRes)
            analysisResultItemRiskBadge.visibility = if (item.isRisk) android.view.View.VISIBLE else android.view.View.GONE
            root.setOnClickListener { item.destination?.let(onItemClicked) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<AnalysisResultItem>() {
        override fun areItemsTheSame(oldItem: AnalysisResultItem, newItem: AnalysisResultItem): Boolean =
            oldItem.titleRes == newItem.titleRes

        override fun areContentsTheSame(oldItem: AnalysisResultItem, newItem: AnalysisResultItem): Boolean =
            oldItem == newItem
    }
}
