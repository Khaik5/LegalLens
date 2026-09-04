package com.example.lagallens.presentation.feature.home

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityHomeBinding
import com.example.lagallens.presentation.feature.register.RegisterActivity
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
            viewModel.onEvent(HomeContract.Event.LoginClicked)
        }
        binding.registerButton.setOnClickListener {
            viewModel.onEvent(HomeContract.Event.RegisterClicked)
        }
    }

    private fun collectEffect() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        HomeContract.Effect.NavigateToRegister -> {
                            startActivity(Intent(this@HomeActivity, RegisterActivity::class.java))
                        }
                        HomeContract.Effect.ShowLoginComingSoon -> {
                            Toast.makeText(this@HomeActivity, getString(R.string.login_coming_soon), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}
