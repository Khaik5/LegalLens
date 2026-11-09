package com.example.lagallens.presentation.feature.analysis.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.lagallens.R
import com.example.lagallens.databinding.ItemAnalysisLockedPreviewBinding

class AnalysisLockedPreviewAdapter(private val onUnlock: () -> Unit) : RecyclerView.Adapter<AnalysisLockedPreviewAdapter.Holder>() {
    private var count = 0
    private var cost: Int? = null

    fun update(lockedCount: Int, unlockCost: Int?) {
        if (count == lockedCount && cost == unlockCost) return
        count = lockedCount
        cost = unlockCost
        notifyDataSetChanged()
    }

    override fun getItemCount() = if (count > 0) 1 else 0
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(ItemAnalysisLockedPreviewBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(count, cost, onUnlock)

    class Holder(private val binding: ItemAnalysisLockedPreviewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(count: Int, cost: Int?, onUnlock: () -> Unit) = binding.bindLockedPreview(count, cost, onUnlock)
    }
}

/** Shared by list footers and the parties screen, so every locked card reads the same. */
fun ItemAnalysisLockedPreviewBinding.bindLockedPreview(count: Int, cost: Int?, onUnlock: () -> Unit) {
    val context = root.context
    val canUnlock = cost != null && cost > 0
    analysisLockedCount.text = context.getString(R.string.analysis_locked_count, count)
    analysisLockedUnlock.isEnabled = canUnlock
    analysisLockedUnlock.text = if (canUnlock) context.getString(R.string.analysis_legal_unlock, cost) else context.getString(R.string.analysis_unlock_price_unavailable)
    analysisLockedUnlock.setOnClickListener { onUnlock() }
}
