package com.example.lagallens.presentation.feature.home.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import com.example.lagallens.databinding.ActivityHomeBinding
import com.example.lagallens.presentation.feature.home.contract.HomeUiEffect
import com.example.lagallens.presentation.feature.home.contract.HomeUiEvent
import com.example.lagallens.presentation.feature.home.viewmodel.HomeViewModel
import com.example.lagallens.presentation.feature.login.ui.LoginActivity
import com.example.lagallens.presentation.feature.register.ui.RegisterActivity
import com.example.lagallens.presentation.common.utils.MotionTokens
import com.example.lagallens.presentation.common.utils.animateContentEntrance
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {
    private val binding by lazy { ActivityHomeBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<HomeViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        playEntranceMotion()
        bindView()
        collectEffect()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun bindView() {
        binding.loginButton.bindPressFeedback()
        binding.registerButton.bindPressFeedback()
        binding.loginButton.setOnClickListener {
            viewModel.onEvent(HomeUiEvent.LoginClicked)
        }
        binding.registerButton.setOnClickListener {
            viewModel.onEvent(HomeUiEvent.RegisterClicked)
        }
    }

    private fun playEntranceMotion() {
        binding.homeRoot.doOnPreDraw {
            val offset = resources.getDimension(com.example.lagallens.R.dimen.dp_16)
            binding.logoImage.animateContentEntrance(offset)
            binding.taglineText.animateContentEntrance(
                translationDistanceY = offset,
                startDelayMillis = MotionTokens.FAST_DURATION_MILLIS
            )
            binding.loginButton.animateContentEntrance(
                translationDistanceY = offset,
                startDelayMillis = MotionTokens.FAST_DURATION_MILLIS * 2
            )
            binding.registerButton.animateContentEntrance(
                translationDistanceY = offset,
                startDelayMillis = MotionTokens.FAST_DURATION_MILLIS * 3
            )
        }
    }

    private fun collectEffect() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEffect.collect { effect ->
                    when (effect) {
                        HomeUiEffect.NavigateToLogin -> {
                            startActivity(Intent(this@HomeActivity, LoginActivity::class.java))
                        }
                        HomeUiEffect.NavigateToRegister -> {
                            startActivity(Intent(this@HomeActivity, RegisterActivity::class.java))
                        }
                    }
                }
            }
        }
    }
}
