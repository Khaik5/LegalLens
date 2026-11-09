package com.example.lagallens.presentation.feature.analysis.obligations.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lagallens.databinding.ItemAnalysisObligationBinding
import com.example.lagallens.databinding.ItemAnalysisObligationsSectionBinding
import com.example.lagallens.presentation.feature.analysis.obligations.contract.AnalysisObligation
import com.example.lagallens.presentation.feature.analysis.obligations.contract.AnalysisObligationsListItem

class AnalysisObligationsAdapter(private val onSourceClicked: (String) -> Unit = {}) :
    ListAdapter<AnalysisObligationsListItem, RecyclerView.ViewHolder>(DiffCallback) {

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is AnalysisObligationsListItem.Section -> VIEW_TYPE_SECTION
        is AnalysisObligationsListItem.Obligation -> VIEW_TYPE_OBLIGATION
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = when (viewType) {
        VIEW_TYPE_SECTION -> SectionViewHolder(
            ItemAnalysisObligationsSectionBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

        else -> ObligationViewHolder(
            ItemAnalysisObligationBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is AnalysisObligationsListItem.Section -> (holder as SectionViewHolder).bind(item)
            is AnalysisObligationsListItem.Obligation -> (holder as ObligationViewHolder).bind(item.obligation, onSourceClicked)
        }
    }

    class SectionViewHolder(
        private val binding: ItemAnalysisObligationsSectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AnalysisObligationsListItem.Section) = with(binding) {
            analysisObligationsSectionTitle.text = item.title
        }
    }

    class ObligationViewHolder(
        private val binding: ItemAnalysisObligationBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AnalysisObligation, onSourceClicked: (String) -> Unit) = with(binding) {
            analysisObligationTitle.text = item.title
            analysisObligationClause.text = item.clause
            analysisObligationDescription.text = item.description
            analysisObligationSource.setOnClickListener { onSourceClicked(item.id) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<AnalysisObligationsListItem>() {
        override fun areItemsTheSame(
            oldItem: AnalysisObligationsListItem,
            newItem: AnalysisObligationsListItem
        ): Boolean = when {
            oldItem is AnalysisObligationsListItem.Section && newItem is AnalysisObligationsListItem.Section ->
                oldItem.id == newItem.id

            oldItem is AnalysisObligationsListItem.Obligation && newItem is AnalysisObligationsListItem.Obligation ->
                oldItem.obligation.id == newItem.obligation.id

            else -> false
        }

        override fun areContentsTheSame(
            oldItem: AnalysisObligationsListItem,
            newItem: AnalysisObligationsListItem
        ): Boolean = oldItem == newItem
    }

    private companion object {
        const val VIEW_TYPE_SECTION = 1
        const val VIEW_TYPE_OBLIGATION = 2
    }
}
