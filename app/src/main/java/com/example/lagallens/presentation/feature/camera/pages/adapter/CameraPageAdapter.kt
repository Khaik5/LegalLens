package com.example.lagallens.presentation.feature.camera.pages.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.lagallens.databinding.ItemCameraPageBinding
import com.example.lagallens.domain.model.camera.ScanPage
import java.io.File

class CameraPageAdapter(
    private val onDeleteClicked: (ScanPage) -> Unit
) : RecyclerView.Adapter<CameraPageAdapter.PageViewHolder>() {
    private var pages: List<ScanPage> = emptyList()

    fun submitPages(pages: List<ScanPage>) {
        this.pages = pages
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        return PageViewHolder(
            ItemCameraPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(pages[position], position + 1)
    }

    override fun getItemCount(): Int = pages.size

    inner class PageViewHolder(
        private val binding: ItemCameraPageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(page: ScanPage, position: Int) = with(binding) {
            Glide.with(root).load(File(page.path)).centerCrop().into(cameraPagePreview)
            cameraPageNumber.text = position.toString()
            cameraPageWarning.visibility = if (page.hasQualityWarning) android.view.View.VISIBLE else android.view.View.GONE
            cameraPageDelete.setOnClickListener { onDeleteClicked(page) }
        }
    }
}
