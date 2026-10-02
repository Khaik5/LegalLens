package com.example.lagallens.presentation.feature.analysis.legalreferences.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lagallens.databinding.ItemAnalysisLegalReferenceBinding
import com.example.lagallens.presentation.feature.analysis.legalreferences.model.AnalysisLegalReference

class AnalysisLegalReferencesAdapter(
    private val onReferenceClicked: (String) -> Unit
) : ListAdapter<AnalysisLegalReference, AnalysisLegalReferencesAdapter.ViewHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        ItemAnalysisLegalReferenceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onReferenceClicked)
    }

    class ViewHolder(
        private val binding: ItemAnalysisLegalReferenceBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            reference: AnalysisLegalReference,
            onReferenceClicked: (String) -> Unit
        ) = with(binding) {
            analysisLegalReferenceTitle.setText(reference.titleRes)
            analysisLegalReferenceCountry.setText(reference.countryRes)
            analysisLegalReferenceArticle.setText(reference.articleRes)
            analysisLegalReferenceTopic.setText(reference.topicRes)
            analysisLegalReferenceDescription.setText(reference.descriptionRes)
            analysisLegalReferenceEffectiveDate.setText(reference.effectiveDateRes)
            analysisLegalReferenceFull.setOnClickListener { onReferenceClicked(reference.id) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<AnalysisLegalReference>() {
        override fun areItemsTheSame(
            oldItem: AnalysisLegalReference,
            newItem: AnalysisLegalReference
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: AnalysisLegalReference,
            newItem: AnalysisLegalReference
        ): Boolean = oldItem == newItem
    }
}
