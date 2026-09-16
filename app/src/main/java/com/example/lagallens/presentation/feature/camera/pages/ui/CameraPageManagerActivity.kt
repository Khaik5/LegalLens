package com.example.lagallens.presentation.feature.camera.pages.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lagallens.databinding.ActivityCameraPageManagerBinding
import com.example.lagallens.domain.model.camera.ScanPage
import com.example.lagallens.presentation.feature.camera.ocr.ui.OcrProcessingActivity
import com.example.lagallens.presentation.feature.camera.pages.adapter.CameraPageAdapter
import com.example.lagallens.presentation.feature.camera.pages.contract.CameraPageManagerUiEffect
import com.example.lagallens.presentation.feature.camera.pages.contract.CameraPageManagerUiEvent
import com.example.lagallens.presentation.feature.camera.pages.contract.CameraPageManagerUiState
import com.example.lagallens.presentation.feature.camera.pages.viewmodel.CameraPageManagerViewModel
import kotlinx.coroutines.launch
import java.io.File

class CameraPageManagerActivity : AppCompatActivity() {
    private val binding by lazy { ActivityCameraPageManagerBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<CameraPageManagerViewModel>()
    private val pageAdapter = CameraPageAdapter { page ->
        File(page.path).delete()
        viewModel.onEvent(CameraPageManagerUiEvent.DeletePageClicked(page.path))
    }

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
        initializePages()
        initializePageList()
        bindView()
        bindBackPressedDispatcher()
        collectViewModel()
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.cameraPageManagerRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initializePages() {
        val warningPaths = intent.getStringArrayListExtra(EXTRA_WARNING_PATHS).orEmpty().toSet()
        viewModel.setPages(
            intent.getStringArrayListExtra(EXTRA_PAGE_PATHS).orEmpty().map { ScanPage(it, it in warningPaths) }
        )
    }

    private fun initializePageList() {
        binding.cameraPageList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.cameraPageList.adapter = pageAdapter
    }

    private fun bindView() {
        binding.cameraPageBack.setOnClickListener { viewModel.onEvent(CameraPageManagerUiEvent.BackClicked) }
        binding.cameraPageClearAll.setOnClickListener { viewModel.onEvent(CameraPageManagerUiEvent.ClearAllClicked) }
        binding.cameraPageAdd.setOnClickListener { viewModel.onEvent(CameraPageManagerUiEvent.AddPageClicked) }
        binding.cameraPageRetakeLast.setOnClickListener { viewModel.onEvent(CameraPageManagerUiEvent.RetakeLastClicked) }
        binding.cameraPageComplete.setOnClickListener { viewModel.onEvent(CameraPageManagerUiEvent.CompleteClicked) }
    }

    private fun collectViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.uiEffect.collect(::handleEffect) }
            }
        }
    }

    private fun bindBackPressedDispatcher() {
        onBackPressedDispatcher.addCallback(this) {
            viewModel.onEvent(CameraPageManagerUiEvent.BackClicked)
        }
    }

    private fun render(state: CameraPageManagerUiState) {
        pageAdapter.submitPages(state.pages)
        binding.cameraPageCount.text = state.pages.size.toString()
        binding.cameraPageComplete.isEnabled = state.pages.isNotEmpty()
        binding.cameraPageRetakeLast.isEnabled = state.pages.isNotEmpty()
        binding.cameraPageClearAll.isEnabled = state.pages.isNotEmpty()
    }

    private fun handleEffect(effect: CameraPageManagerUiEffect) {
        when (effect) {
            is CameraPageManagerUiEffect.ReturnToCapture -> {
                effect.deletedPaths.forEach { File(it).delete() }
                setResult(
                    RESULT_OK,
                    Intent()
                        .putStringArrayListExtra(EXTRA_PAGE_PATHS, ArrayList(effect.pages.map(ScanPage::path)))
                        .putStringArrayListExtra(
                            EXTRA_WARNING_PATHS,
                            ArrayList(effect.pages.filter(ScanPage::hasQualityWarning).map(ScanPage::path))
                        )
                )
                finish()
            }
            is CameraPageManagerUiEffect.NavigateToOcrProcessing -> {
                startActivity(OcrProcessingActivity.createIntent(this, effect.pages))
            }
        }
    }

    companion object {
        const val EXTRA_PAGE_PATHS = "camera_page_paths"
        const val EXTRA_WARNING_PATHS = "camera_warning_paths"

        fun createIntent(context: Context, pages: List<ScanPage>): Intent = Intent(
            context,
            CameraPageManagerActivity::class.java
        ).putStringArrayListExtra(EXTRA_PAGE_PATHS, ArrayList(pages.map(ScanPage::path)))
            .putStringArrayListExtra(
                EXTRA_WARNING_PATHS,
                ArrayList(pages.filter(ScanPage::hasQualityWarning).map(ScanPage::path))
            )
    }
}
