package com.example.lagallens.presentation.feature.splash.ui

import android.os.Bundle
import android.content.Intent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.R
import com.example.lagallens.data.datasource.local.datastore.OnboardingPreferencesDataSource
import com.example.lagallens.databinding.ActivitySplashBinding
import com.example.lagallens.presentation.feature.splash.contract.SplashEffect
import com.example.lagallens.presentation.feature.splash.contract.SplashEvent
import com.example.lagallens.presentation.feature.splash.viewmodel.SplashViewModel
import com.example.lagallens.presentation.feature.onboarding.ui.OnboardingActivity
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {
    private val binding by lazy { ActivitySplashBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<SplashViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        window.statusBarColor = getColor(R.color.splash_background)
        window.navigationBarColor = getColor(R.color.splash_background)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effects.collect(::handleEffect)
            }
        }

        viewModel.onEvent(SplashEvent.ScreenStarted)
    }

    private fun handleEffect(effect: SplashEffect) {
        when (effect) {
            SplashEffect.OpenOnboarding -> {
                lifecycleScope.launch {
                    if (OnboardingPreferencesDataSource(applicationContext).isCompleted()) {
                        finish()
                    } else {
                        startActivity(Intent(this@SplashActivity, OnboardingActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }
}
