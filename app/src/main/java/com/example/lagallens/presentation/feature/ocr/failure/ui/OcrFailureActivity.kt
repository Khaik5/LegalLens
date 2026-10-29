package com.example.lagallens.presentation.feature.ocr.failure.ui

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
import com.example.lagallens.databinding.ActivityOcrFailureBinding
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import com.example.lagallens.presentation.feature.camera.ocr.ui.OcrProcessingActivity
import com.example.lagallens.presentation.feature.ocr.failure.contract.OcrFailureUiEffect
import com.example.lagallens.presentation.feature.ocr.failure.contract.OcrFailureUiEvent
import com.example.lagallens.presentation.feature.ocr.failure.viewmodel.OcrFailureViewModel
import kotlinx.coroutines.launch

class OcrFailureActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOcrFailureBinding
    private val viewModel by viewModels<OcrFailureViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityOcrFailureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.ocrFailureRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
        bindEvents()
        observeViewModel()
        viewModel.onEvent(
            OcrFailureUiEvent.ScreenOpened(
                documentName = intent.getStringExtra(EXTRA_DOCUMENT_NAME).orEmpty(),
                documentDetails = intent.getStringExtra(EXTRA_DOCUMENT_DETAILS).orEmpty()
            )
        )
    }

    private fun bindEvents() = with(binding) {
        listOf(ocrFailureBack, ocrFailureReturn, ocrFailureRetry).forEach { it.bindPressFeedback() }
        ocrFailureBack.setOnClickListener { viewModel.onEvent(OcrFailureUiEvent.BackClicked) }
        ocrFailureReturn.setOnClickListener { viewModel.onEvent(OcrFailureUiEvent.BackClicked) }
        ocrFailureRetry.setOnClickListener { viewModel.onEvent(OcrFailureUiEvent.RetryClicked) }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiEffect.collect(::handleEffect) }
            }
        }
    }

    private fun handleEffect(effect: OcrFailureUiEffect) {
        when (effect) {
            OcrFailureUiEffect.NavigateBack -> finish()
            is OcrFailureUiEffect.RetryOcr -> {
                startActivity(
                    OcrProcessingActivity.createIntent(
                        context = this,
                        documentName = effect.documentName,
                        documentDetails = effect.documentDetails
                    )
                )
                finish()
            }
        }
    }

    companion object {
        private const val EXTRA_DOCUMENT_NAME = "ocr_failure_document_name"
        private const val EXTRA_DOCUMENT_DETAILS = "ocr_failure_document_details"

        fun createIntent(
            context: Context,
            documentName: String,
            documentDetails: String
        ): Intent = Intent(context, OcrFailureActivity::class.java)
            .putExtra(EXTRA_DOCUMENT_NAME, documentName)
            .putExtra(EXTRA_DOCUMENT_DETAILS, documentDetails)
    }
}
