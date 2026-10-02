package com.example.lagallens.presentation.common.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lagallens.R
import com.example.lagallens.databinding.ItemProcessingTimelineStepBinding
import com.example.lagallens.presentation.common.model.ProcessingTimelineStep
import com.example.lagallens.presentation.common.model.ProcessingTimelineStepStatus

class ProcessingTimelineStepAdapter :
    ListAdapter<ProcessingTimelineStep, ProcessingTimelineStepAdapter.ProcessingTimelineStepViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProcessingTimelineStepViewHolder {
        val binding = ItemProcessingTimelineStepBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProcessingTimelineStepViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProcessingTimelineStepViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ProcessingTimelineStepViewHolder(
        private val binding: ItemProcessingTimelineStepBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ProcessingTimelineStep) = with(binding) {
            processingTimelineStepConnector.isVisible = item.hasConnector
            processingTimelineStepConnector.setBackgroundColor(
                ContextCompat.getColor(root.context, item.connectorColorRes)
            )
            processingTimelineStepCompletedIndicator.isVisible = item.status == ProcessingTimelineStepStatus.COMPLETED
            processingTimelineStepCompletedIcon.isVisible = item.status == ProcessingTimelineStepStatus.COMPLETED
            processingTimelineStepActiveIndicator.isVisible = item.status == ProcessingTimelineStepStatus.ACTIVE
            processingTimelineStepActiveDot.isVisible = item.status == ProcessingTimelineStepStatus.ACTIVE
            processingTimelineStepPendingIndicator.isVisible = item.status == ProcessingTimelineStepStatus.PENDING
            processingTimelineStepPendingIndicator.text = item.id.toString()
            processingTimelineStepTitle.setText(item.titleRes)
            processingTimelineStepDescription.setText(item.descriptionRes)

            val titleColorRes = when (item.status) {
                ProcessingTimelineStepStatus.ACTIVE -> R.color.ocr_saved_teal
                ProcessingTimelineStepStatus.COMPLETED -> R.color.ocr_saved_navy
                ProcessingTimelineStepStatus.PENDING -> R.color.ocr_saved_body
            }
            val descriptionColorRes = when (item.status) {
                ProcessingTimelineStepStatus.ACTIVE -> R.color.ocr_saved_teal
                ProcessingTimelineStepStatus.COMPLETED,
                ProcessingTimelineStepStatus.PENDING -> R.color.ocr_saved_body
            }
            processingTimelineStepTitle.setTextColor(ContextCompat.getColor(root.context, titleColorRes))
            processingTimelineStepDescription.setTextColor(
                ContextCompat.getColor(root.context, descriptionColorRes)
            )
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ProcessingTimelineStep>() {
        override fun areItemsTheSame(
            oldItem: ProcessingTimelineStep,
            newItem: ProcessingTimelineStep
        ) = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: ProcessingTimelineStep,
            newItem: ProcessingTimelineStep
        ) = oldItem == newItem
    }
}
