package com.example.lagallens.presentation.feature.onboarding.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.recyclerview.widget.RecyclerView
import com.example.lagallens.R
import com.example.lagallens.databinding.ItemOnboardingPageBinding
import com.example.lagallens.presentation.common.utils.MotionTokens
import com.example.lagallens.presentation.common.utils.animateContentEntrance
import com.example.lagallens.presentation.feature.onboarding.contract.OnboardingPage

class OnboardingPagerAdapter(
    private val pages: List<OnboardingPage>
) : RecyclerView.Adapter<OnboardingPagerAdapter.PageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemOnboardingPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(pages[position])
    }

    override fun getItemCount(): Int = pages.size

    class PageViewHolder(
        private val binding: ItemOnboardingPageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(page: OnboardingPage) = with(binding) {
            onboardingIllustration.setImageResource(page.imageResId)
            onboardingTitle.setText(page.titleResId)
            onboardingDescription.setText(page.descriptionResId)
            root.doOnPreDraw {
                val offset = root.resources.getDimension(R.dimen.dp_16)
                onboardingIllustration.animateContentEntrance(offset)
                onboardingTitle.animateContentEntrance(
                    translationDistanceY = offset,
                    startDelayMillis = MotionTokens.FAST_DURATION_MILLIS
                )
                onboardingDescription.animateContentEntrance(
                    translationDistanceY = offset,
                    startDelayMillis = MotionTokens.FAST_DURATION_MILLIS * 2
                )
            }
        }
    }
}
