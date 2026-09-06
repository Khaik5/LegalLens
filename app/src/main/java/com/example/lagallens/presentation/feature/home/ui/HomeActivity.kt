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
import com.example.lagallens.databinding.ActivityHomeBinding
import com.example.lagallens.presentation.feature.home.contract.HomeUiEffect
import com.example.lagallens.presentation.feature.home.contract.HomeUiEvent
import com.example.lagallens.presentation.feature.home.viewmodel.HomeViewModel
import com.example.lagallens.presentation.feature.login.ui.LoginActivity
import com.example.lagallens.presentation.feature.register.ui.RegisterActivity
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {
    private val binding by lazy { ActivityHomeBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<HomeViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        bindView()
        collectEffect()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun bindView() {
        binding.loginButton.setOnClickListener {
            viewModel.onEvent(HomeUiEvent.LoginClicked)
        }
        binding.registerButton.setOnClickListener {
            viewModel.onEvent(HomeUiEvent.RegisterClicked)
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
