package com.example.lagallens.presentation.feature.camera.ocr.ui

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
import com.example.lagallens.databinding.ActivityOcrProcessingBinding
import com.example.lagallens.domain.model.camera.ScanPage
import com.example.lagallens.presentation.feature.camera.ocr.contract.OcrProcessingUiState
import com.example.lagallens.presentation.feature.camera.ocr.viewmodel.OcrProcessingViewModel
import kotlinx.coroutines.launch

class OcrProcessingActivity : AppCompatActivity() {
    private val binding by lazy { ActivityOcrProcessingBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<OcrProcessingViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContentView(binding.root)
        applyWindowInsets()
        initializePageCount()
        bindView()
        collectState()
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.ocrProcessingRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initializePageCount() {
        viewModel.setPageCount(intent.getStringArrayListExtra(EXTRA_PAGE_PATHS).orEmpty().size)
    }

    private fun bindView() {
        binding.ocrProcessingBack.setOnClickListener { finish() }
    }

    private fun collectState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: OcrProcessingUiState) {
        binding.ocrProcessingPageCount.text = getString(R.string.ocr_processing_page_count, state.pageCount)
    }

    companion object {
        private const val EXTRA_PAGE_PATHS = "ocr_page_paths"

        fun createIntent(context: Context, pages: List<ScanPage>): Intent = Intent(
            context,
            OcrProcessingActivity::class.java
        ).putStringArrayListExtra(EXTRA_PAGE_PATHS, ArrayList(pages.map(ScanPage::path)))
    }
}
