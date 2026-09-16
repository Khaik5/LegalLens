package com.example.lagallens.presentation.feature.success.ui

import android.content.Intent
import android.animation.AnimatorSet
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.databinding.ActivitySuccessBinding
import com.example.lagallens.presentation.feature.main.ui.MainActivity
import com.example.lagallens.presentation.feature.success.contract.SuccessUiEffect
import com.example.lagallens.presentation.feature.success.contract.SuccessUiEvent
import com.example.lagallens.presentation.assets.animation.SuccessPulseAnimation
import com.example.lagallens.presentation.feature.success.viewmodel.SuccessViewModel
import kotlinx.coroutines.launch

class SuccessActivity : AppCompatActivity() {
    private val binding by lazy { ActivitySuccessBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<SuccessViewModel>()
    private var successPulseAnimation: AnimatorSet? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        binding.startUsingButton.setOnClickListener {
            viewModel.onEvent(SuccessUiEvent.StartUsingClicked)
        }
        collectEffect()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun collectEffect() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEffect.collect { effect ->
                    when (effect) {
                        SuccessUiEffect.NavigateToHome -> {
                            startActivity(
                                Intent(this@SuccessActivity, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        successPulseAnimation = SuccessPulseAnimation.start(binding.successSeal)
    }

    override fun onPause() {
        successPulseAnimation?.cancel()
        successPulseAnimation = null
        binding.successSeal.scaleX = 1f
        binding.successSeal.scaleY = 1f
        super.onPause()
    }
}
