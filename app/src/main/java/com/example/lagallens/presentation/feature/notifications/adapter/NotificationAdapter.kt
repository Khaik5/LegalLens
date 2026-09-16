package com.example.lagallens.presentation.feature.notifications.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lagallens.R
import com.example.lagallens.databinding.ItemNotificationBinding
import com.example.lagallens.presentation.feature.notifications.contract.NotificationItem

class NotificationAdapter(
    private val onNotificationClicked: (NotificationItem) -> Unit
) : ListAdapter<NotificationItem, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding, onNotificationClicked)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NotificationViewHolder(
        private val binding: ItemNotificationBinding,
        private val onNotificationClicked: (NotificationItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: NotificationItem) {
            val context = binding.root.context
            binding.root.setBackgroundResource(
                if (item.isUnread) R.drawable.bg_notification_card_unread else R.drawable.bg_notification_card
            )
            binding.ivNotificationIcon.setImageResource(item.iconRes)
            binding.ivNotificationIcon.setBackgroundResource(item.iconBackgroundRes)
            binding.ivNotificationIcon.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, item.iconTintRes)
            )
            binding.tvTitle.setText(item.titleRes)
            binding.tvDescription.setText(item.descriptionRes)
            binding.tvTime.setText(item.timeRes)
            binding.viewUnread.isVisible = item.isUnread
            binding.root.setOnClickListener { onNotificationClicked(item) }
        }
    }

    private object NotificationDiffCallback : DiffUtil.ItemCallback<NotificationItem>() {
        override fun areItemsTheSame(oldItem: NotificationItem, newItem: NotificationItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NotificationItem, newItem: NotificationItem): Boolean {
            return oldItem == newItem
        }
    }
}
