package com.example.lagallens.presentation.feature.splash.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivitySplashBinding
import com.example.lagallens.presentation.feature.home.ui.HomeActivity
import com.example.lagallens.presentation.feature.main.ui.MainActivity
import com.example.lagallens.presentation.feature.onboarding.ui.OnboardingActivity
import com.example.lagallens.presentation.feature.splash.contract.SplashUiEffect
import com.example.lagallens.presentation.feature.splash.contract.SplashUiEvent
import com.example.lagallens.presentation.feature.splash.viewmodel.SplashViewModel
import com.example.lagallens.presentation.feature.splash.utils.SplashMotion
import com.example.lagallens.presentation.feature.splash.utils.SplashMotion.SplashProgressAnimation
import com.example.lagallens.presentation.common.utils.MotionTokens
import com.example.lagallens.presentation.common.utils.animateContentEntrance
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {
    private val binding by lazy { ActivitySplashBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<SplashViewModel> {
        SplashViewModel.Factory(
            (application as LegalLensApplication).isOnboardingCompletedUseCase,
            com.example.lagallens.di.RepositoryModule.provideAuthRepository(applicationContext)
        )
    }
    private var splashProgressAnimation: SplashProgressAnimation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        window.statusBarColor = getColor(R.color.splash_background)
        window.navigationBarColor = getColor(R.color.splash_background)

        playEntranceMotion()
        collectEffect()
        viewModel.onEvent(SplashUiEvent.ScreenStarted)
    }

    override fun onResume() {
        super.onResume()
        if (splashProgressAnimation == null) {
            splashProgressAnimation = SplashMotion.startProgress(binding.splashProgressIndicator)
        }
    }

    private fun playEntranceMotion() {
        binding.splash.doOnPreDraw {
            val offset = resources.getDimension(R.dimen.dp_16)
            binding.splashLogo.animateContentEntrance(translationDistanceY = offset)
            binding.splashProgress.animateContentEntrance(
                translationDistanceY = offset,
                startDelayMillis = MotionTokens.FAST_DURATION_MILLIS
            )
            binding.splashStatus.animateContentEntrance(
                translationDistanceY = offset,
                startDelayMillis = MotionTokens.FAST_DURATION_MILLIS * 2
            )
        }
    }

    override fun onPause() {
        splashProgressAnimation?.cancel()
        splashProgressAnimation = null
        super.onPause()
    }

    private fun collectEffect() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEffect.collect(::handleEffect)
            }
        }
    }

    private fun handleEffect(effect: SplashUiEffect) {
        when (effect) {
            SplashUiEffect.NavigateToOnboarding -> startActivity(
                Intent(this, OnboardingActivity::class.java)
            ).also { finish() }
            SplashUiEffect.NavigateToHome -> startActivity(
                Intent(this, HomeActivity::class.java)
            ).also { finish() }
            SplashUiEffect.NavigateToMain -> startActivity(
                Intent(this, MainActivity::class.java)
            ).also { finish() }
        }
    }
}
