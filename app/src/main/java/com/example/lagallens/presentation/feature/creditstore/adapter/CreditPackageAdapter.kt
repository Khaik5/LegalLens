package com.example.lagallens.presentation.feature.creditstore.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lagallens.R
import com.example.lagallens.databinding.ItemCreditStorePackageBinding
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import com.example.lagallens.presentation.feature.creditstore.contract.CreditPackage

class CreditPackageAdapter(
    private val onPackageClicked: (CreditPackage) -> Unit
) : ListAdapter<CreditPackage, CreditPackageAdapter.CreditPackageViewHolder>(CreditPackageDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CreditPackageViewHolder {
        val binding = ItemCreditStorePackageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CreditPackageViewHolder(binding, onPackageClicked)
    }

    override fun onBindViewHolder(holder: CreditPackageViewHolder, position: Int) {
        holder.bind(getItem(position), position == itemCount - 1)
    }

    class CreditPackageViewHolder(
        private val binding: ItemCreditStorePackageBinding,
        private val onPackageClicked: (CreditPackage) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        init { binding.root.bindPressFeedback() }
        fun bind(item: CreditPackage, isLastItem: Boolean) = with(binding) {
            root.setBackgroundResource(
                if (item.isSelected) {
                    R.drawable.bg_credit_store_package_selected
                } else {
                    R.drawable.bg_credit_store_package
                }
            )
            creditStorePackageSelection.setBackgroundResource(
                if (item.isSelected) {
                    R.drawable.bg_credit_store_package_selection_selected
                } else {
                    R.drawable.bg_credit_store_package_selection
                }
            )
            creditStorePackageCheck.isVisible = item.isSelected
            creditStorePackageTitle.setText(item.titleRes)
            creditStorePackageDescription.setText(item.descriptionRes)
            creditStorePackageCredits.text = item.credits.toString()
            root.isSelected = item.isSelected
            (root.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin = if (isLastItem) {
                0
            } else {
                root.resources.getDimensionPixelSize(R.dimen.dp_10)
            }
            root.setOnClickListener { onPackageClicked(item) }
        }
    }

    private object CreditPackageDiffCallback : DiffUtil.ItemCallback<CreditPackage>() {
        override fun areItemsTheSame(oldItem: CreditPackage, newItem: CreditPackage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CreditPackage, newItem: CreditPackage): Boolean {
            return oldItem == newItem
        }
    }
}
