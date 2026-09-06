package com.example.lagallens.presentation.feature.onboarding.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.example.lagallens.R
import com.example.lagallens.data.datasource.local.datastore.OnboardingPreferencesDataSource
import com.example.lagallens.databinding.ActivityOnboardingBinding
import com.example.lagallens.presentation.feature.onboarding.adapter.OnboardingPagerAdapter
import com.example.lagallens.presentation.feature.onboarding.contract.OnboardingEffect
import com.example.lagallens.presentation.feature.onboarding.contract.OnboardingEvent
import com.example.lagallens.presentation.feature.onboarding.contract.OnboardingUiState
import com.example.lagallens.presentation.feature.onboarding.viewmodel.OnboardingViewModel
import kotlinx.coroutines.launch

class OnboardingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOnboardingBinding
    private val viewModel by viewModels<OnboardingViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.statusBarColor = getColor(R.color.onboarding_background)
        window.navigationBarColor = getColor(R.color.onboarding_background)

        binding.onboardingPager.adapter = OnboardingPagerAdapter(viewModel.uiState.value.pages)
        binding.onboardingSkip.setOnClickListener {
            viewModel.onEvent(OnboardingEvent.SkipClicked)
        }
        binding.onboardingNext.setOnClickListener {
            viewModel.onEvent(OnboardingEvent.NextClicked)
        }
        binding.onboardingPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.onEvent(OnboardingEvent.PageSelected(position))
            }
        })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.effects.collect(::handleEffect) }
            }
        }
    }

    private fun render(state: OnboardingUiState) {
        binding.onboardingNext.setText(state.page.actionResId)
        updateDots(state.pageIndex)
        if (binding.onboardingPager.currentItem != state.pageIndex) {
            binding.onboardingPager.setCurrentItem(state.pageIndex, true)
        }
    }

    private fun updateDots(pageIndex: Int) {
        binding.onboardingDotOne.setBackgroundResource(
            if (pageIndex == 0) R.drawable.onboarding_dot_active else R.drawable.onboarding_dot_inactive
        )
        binding.onboardingDotTwo.setBackgroundResource(
            if (pageIndex == 1) R.drawable.onboarding_dot_active else R.drawable.onboarding_dot_inactive
        )
        binding.onboardingDotThree.setBackgroundResource(
            if (pageIndex == 2) R.drawable.onboarding_dot_active else R.drawable.onboarding_dot_inactive
        )
    }

    private fun handleEffect(effect: OnboardingEffect) {
        when (effect) {
            OnboardingEffect.StartRequested, OnboardingEffect.SkipRequested -> {
                lifecycleScope.launch {
                    OnboardingPreferencesDataSource(applicationContext).markCompleted()
                    setResult(RESULT_OK)
                    finish()
                }
            }
        }
    }
}
