package com.example.lagallens.presentation.feature.onboarding.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityOnboardingBinding
import com.example.lagallens.presentation.feature.home.ui.HomeActivity
import com.example.lagallens.presentation.feature.onboarding.adapter.OnboardingPagerAdapter
import com.example.lagallens.presentation.feature.onboarding.contract.OnboardingUiEffect
import com.example.lagallens.presentation.feature.onboarding.contract.OnboardingUiEvent
import com.example.lagallens.presentation.feature.onboarding.contract.OnboardingUiState
import com.example.lagallens.presentation.feature.onboarding.viewmodel.OnboardingViewModel
import kotlinx.coroutines.launch

class OnboardingActivity : AppCompatActivity() {
    private val binding by lazy { ActivityOnboardingBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<OnboardingViewModel> {
        OnboardingViewModel.Factory(
            (application as LegalLensApplication).completeOnboardingUseCase
        )
    }
    private val onboardingAdapter by lazy { OnboardingPagerAdapter(viewModel.uiState.value.pages) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        window.statusBarColor = getColor(R.color.onboarding_background)
        window.navigationBarColor = getColor(R.color.onboarding_background)

        setUpPager()
        bindView()
        collectViewModel()
    }

    private fun setUpPager() {
        binding.onboardingPager.adapter = onboardingAdapter
        binding.onboardingPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.onEvent(OnboardingUiEvent.PageSelected(position))
            }
        })
    }

    private fun bindView() {
        binding.onboardingSkip.setOnClickListener {
            viewModel.onEvent(OnboardingUiEvent.SkipClicked)
        }
        binding.onboardingNext.setOnClickListener {
            viewModel.onEvent(OnboardingUiEvent.NextClicked)
        }
    }

    private fun collectViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.uiEffect.collect(::handleEffect) }
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

    private fun handleEffect(effect: OnboardingUiEffect) {
        when (effect) {
            OnboardingUiEffect.NavigateToHome -> startActivity(
                Intent(this, HomeActivity::class.java)
            ).also { finish() }
        }
    }
}
