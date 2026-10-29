package com.example.lagallens.presentation.feature.onboarding.ui

import android.content.Intent
import android.animation.ValueAnimator
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.core.view.doOnPreDraw
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
import com.example.lagallens.presentation.feature.onboarding.utils.OnboardingIndicatorMotion
import com.example.lagallens.presentation.feature.onboarding.viewmodel.OnboardingViewModel
import com.example.lagallens.presentation.common.utils.MotionTokens
import com.example.lagallens.presentation.common.utils.animateContentEntrance
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import kotlinx.coroutines.launch

class OnboardingActivity : AppCompatActivity() {
    private val binding by lazy { ActivityOnboardingBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<OnboardingViewModel> {
        OnboardingViewModel.Factory(
            (application as LegalLensApplication).completeOnboardingUseCase
        )
    }
    private val onboardingAdapter by lazy { OnboardingPagerAdapter(viewModel.uiState.value.pages) }
    private val indicatorAnimations = arrayOfNulls<ValueAnimator>(INDICATOR_COUNT)
    private val indicatorColors = IntArray(INDICATOR_COUNT)
    private var hasRenderedIndicator = false
    private var isNavigating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        window.statusBarColor = getColor(R.color.onboarding_background)
        window.navigationBarColor = getColor(R.color.onboarding_background)

        playEntranceMotion()
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
        binding.onboardingSkip.bindPressFeedback()
        binding.onboardingNext.bindPressFeedback()
        binding.onboardingSkip.setOnClickListener {
            viewModel.onEvent(OnboardingUiEvent.SkipClicked)
        }
        binding.onboardingNext.setOnClickListener {
            viewModel.onEvent(OnboardingUiEvent.NextClicked)
        }
    }

    private fun playEntranceMotion() {
        binding.onboardingRoot.doOnPreDraw {
            val offset = resources.getDimension(R.dimen.dp_16)
            binding.onboardingLogo.animateContentEntrance(offset)
            binding.onboardingBottomContent.animateContentEntrance(
                translationDistanceY = offset,
                startDelayMillis = MotionTokens.FAST_DURATION_MILLIS
            )
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
        val activeColor = getColor(R.color.onboarding_navy)
        val inactiveColor = getColor(R.color.onboarding_gray)
        val activeWidth = resources.getDimensionPixelSize(R.dimen.onboarding_dot_active_width)
        val inactiveWidth = resources.getDimensionPixelSize(R.dimen.onboarding_dot_size)
        val dots = listOf(
            binding.onboardingDotOne,
            binding.onboardingDotTwo,
            binding.onboardingDotThree
        )

        dots.forEachIndexed { index, dot ->
            val isSelected = index == pageIndex
            val targetWidth = if (isSelected) activeWidth else inactiveWidth
            val targetColor = if (isSelected) activeColor else inactiveColor
            indicatorAnimations[index]?.cancel()
            indicatorAnimations[index] = if (hasRenderedIndicator) {
                OnboardingIndicatorMotion.animateToState(
                    view = dot,
                    targetWidth = targetWidth,
                    startColor = indicatorColors[index],
                    targetColor = targetColor
                ) { color ->
                    indicatorColors[index] = color
                }
            } else {
                OnboardingIndicatorMotion.applyState(dot, targetWidth, targetColor)
                indicatorColors[index] = targetColor
                null
            }
        }
        hasRenderedIndicator = true
    }

    private fun handleEffect(effect: OnboardingUiEffect) {
        when (effect) {
            OnboardingUiEffect.NavigateToHome -> if (!isNavigating) {
                isNavigating = true
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }
        }
    }

    override fun onStop() {
        indicatorAnimations.forEach { it?.cancel() }
        hasRenderedIndicator = false
        super.onStop()
    }

    private companion object {
        const val INDICATOR_COUNT = 3
    }
}
