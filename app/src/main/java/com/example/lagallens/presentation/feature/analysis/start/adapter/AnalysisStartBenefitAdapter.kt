package com.example.lagallens.presentation.feature.analysis.start.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lagallens.databinding.ItemAnalysisStartBenefitBinding
import com.example.lagallens.presentation.feature.analysis.start.contract.AnalysisStartBenefit

class AnalysisStartBenefitAdapter :
    ListAdapter<AnalysisStartBenefit, AnalysisStartBenefitAdapter.AnalysisStartBenefitViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnalysisStartBenefitViewHolder {
        val binding = ItemAnalysisStartBenefitBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AnalysisStartBenefitViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AnalysisStartBenefitViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AnalysisStartBenefitViewHolder(
        private val binding: ItemAnalysisStartBenefitBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AnalysisStartBenefit) = with(binding) {
            analysisStartBenefitStep.setText(item.stepRes)
            analysisStartBenefitTitle.setText(item.titleRes)
            analysisStartBenefitDescription.setText(item.descriptionRes)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<AnalysisStartBenefit>() {
        override fun areItemsTheSame(
            oldItem: AnalysisStartBenefit,
            newItem: AnalysisStartBenefit
        ) = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: AnalysisStartBenefit,
            newItem: AnalysisStartBenefit
        ) = oldItem == newItem
    }
}
