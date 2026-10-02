package com.example.lagallens.presentation.feature.upload.error.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityUploadErrorBinding
import com.example.lagallens.presentation.feature.upload.error.contract.UploadErrorEffect
import com.example.lagallens.presentation.feature.upload.error.contract.UploadErrorEvent
import com.example.lagallens.presentation.feature.upload.error.contract.UploadErrorUiState
import com.example.lagallens.presentation.feature.upload.error.viewmodel.UploadErrorViewModel
import com.example.lagallens.presentation.feature.upload.ui.UploadSourceActivity
import kotlinx.coroutines.launch

class UploadErrorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUploadErrorBinding
    private val viewModel by viewModels<UploadErrorViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityUploadErrorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.uploadErrorRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        bindEvents()
        observeViewModel()
        viewModel.onEvent(
            UploadErrorEvent.ScreenOpened(
                errorCode = intent.getStringExtra(EXTRA_ERROR_CODE).orEmpty().ifBlank {
                    UploadErrorUiState.DEFAULT_ERROR_CODE
                },
                errorMessage = intent.getStringExtra(EXTRA_ERROR_MESSAGE).orEmpty().ifBlank {
                    UploadErrorUiState.DEFAULT_ERROR_MESSAGE
                }
            )
        )
    }

    private fun bindEvents() = with(binding) {
        uploadErrorBack.setOnClickListener {
            viewModel.onEvent(UploadErrorEvent.BackClicked)
        }
        uploadErrorRetry.setOnClickListener {
            viewModel.onEvent(UploadErrorEvent.RetryClicked)
        }
        uploadErrorBackToList.setOnClickListener {
            viewModel.onEvent(UploadErrorEvent.BackToListClicked)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.effects.collect(::handleEffect) }
            }
        }
    }

    private fun render(state: UploadErrorUiState) = with(binding) {
        uploadErrorCode.text = getString(R.string.upload_error_code, state.errorCode)
        uploadErrorMessage.text = state.errorMessage
    }

    private fun handleEffect(effect: UploadErrorEffect) {
        when (effect) {
            UploadErrorEffect.OpenUploadSource -> {
                startActivity(
                    Intent(this, UploadSourceActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                )
            }

            UploadErrorEffect.ReturnToCaller -> finishAffinity()
        }
    }

    companion object {
        private const val EXTRA_ERROR_CODE = "extra_error_code"
        private const val EXTRA_ERROR_MESSAGE = "extra_error_message"

        fun newIntent(
            context: Context,
            errorCode: String = UploadErrorUiState.DEFAULT_ERROR_CODE,
            errorMessage: String = UploadErrorUiState.DEFAULT_ERROR_MESSAGE
        ): Intent {
            return Intent(context, UploadErrorActivity::class.java)
                .putExtra(EXTRA_ERROR_CODE, errorCode)
                .putExtra(EXTRA_ERROR_MESSAGE, errorMessage)
        }
    }
}
