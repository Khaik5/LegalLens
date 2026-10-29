package com.example.lagallens.presentation.feature.chatbot.draft.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.lagallens.databinding.ItemChatbotDraftPageBinding
import com.example.lagallens.databinding.ItemChatbotDraftSectionBinding
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftPage

class ChatbotDraftPageAdapter(
    private var pages: List<ChatbotDraftPage>
) : RecyclerView.Adapter<ChatbotDraftPageAdapter.PageViewHolder>() {
    private var zoomPercent = 100

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder = PageViewHolder(
        ItemChatbotDraftPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(pages[position], zoomPercent)
    }

    override fun getItemCount(): Int = pages.size

    fun setZoomPercent(value: Int) {
        zoomPercent = value
        notifyItemRangeChanged(0, itemCount)
    }

    fun setPages(value: List<ChatbotDraftPage>) {
        pages = value
        notifyDataSetChanged()
    }

    class PageViewHolder(
        private val binding: ItemChatbotDraftPageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(page: ChatbotDraftPage, zoomPercent: Int) = with(binding) {
            val context = root.context
            draftPageHeading.isVisible = page.headingRes != null
            page.headingRes?.let { draftPageHeading.setText(it) }
            draftPageIntroduction.isVisible = page.introductionRes != null
            page.introductionRes?.let { draftPageIntroduction.setText(it) }
            draftPageSections.removeAllViews()
            page.sections.forEach { section ->
                val sectionBinding = ItemChatbotDraftSectionBinding.inflate(
                    LayoutInflater.from(context),
                    draftPageSections,
                    false
                )
                sectionBinding.draftSectionTitle.setText(section.titleRes)
                sectionBinding.draftSectionBody.text = section.bodyOverride ?: context.getString(section.bodyRes)
                draftPageSections.addView(sectionBinding.root)
            }
            root.scaleX = zoomPercent / 100f
            root.scaleY = zoomPercent / 100f
        }
    }
}
