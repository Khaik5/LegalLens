package com.example.lagallens.presentation.feature.upload.ui

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.lagallens.databinding.ActivityUploadSourceBinding
import com.example.lagallens.presentation.feature.camera.permission.ui.CameraPermissionActivity
import com.example.lagallens.presentation.feature.upload.contract.UploadSourceUiEffect
import com.example.lagallens.presentation.feature.upload.contract.UploadSourceUiEvent
import com.example.lagallens.presentation.feature.upload.contract.UploadSourceUiState
import com.example.lagallens.presentation.feature.upload.viewmodel.UploadSourceViewModel
import kotlinx.coroutines.launch

class UploadSourceActivity : AppCompatActivity() {
    private val binding by lazy { ActivityUploadSourceBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<UploadSourceViewModel>()
    private val documentPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.onEvent(UploadSourceUiEvent.FileSelected)
    }
    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.onEvent(UploadSourceUiEvent.FileSelected)
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
        bindView()
        collectViewModel()
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.uploadSourceRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun bindView() {
        binding.uploadSourceBack.setOnClickListener {
            viewModel.onEvent(UploadSourceUiEvent.BackClicked)
        }
        binding.uploadSourcePdf.setOnClickListener {
            viewModel.onEvent(UploadSourceUiEvent.PdfClicked)
        }
        binding.uploadSourceDocx.setOnClickListener {
            viewModel.onEvent(UploadSourceUiEvent.DocxClicked)
        }
        binding.uploadSourceGallery.setOnClickListener {
            viewModel.onEvent(UploadSourceUiEvent.GalleryClicked)
        }
        binding.uploadSourceCamera.setOnClickListener {
            viewModel.onEvent(UploadSourceUiEvent.CameraClicked)
        }
    }

    private fun collectViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.uiEffect.collect(::handleEffect) }
            }
        }
    }

    private fun render(state: UploadSourceUiState) {
        binding.uploadSourcePdf.isEnabled = state.isSourceSelectionAvailable
        binding.uploadSourceDocx.isEnabled = state.isSourceSelectionAvailable
        binding.uploadSourceGallery.isEnabled = state.isSourceSelectionAvailable
        binding.uploadSourceCamera.isEnabled = state.isSourceSelectionAvailable
    }

    private fun handleEffect(effect: UploadSourceUiEffect) {
        when (effect) {
            UploadSourceUiEffect.NavigateBack -> finish()
            UploadSourceUiEffect.NavigateToCameraPermission -> startActivity(
                CameraPermissionActivity.createIntent(this)
            )
            is UploadSourceUiEffect.OpenDocumentPicker -> documentPicker.launch(effect.mimeTypes)
            UploadSourceUiEffect.OpenImagePicker -> imagePicker.launch("image/*")
            UploadSourceUiEffect.ShowFileSelected -> showMessage(R.string.upload_source_selected)
        }
    }

    private fun showMessage(messageRes: Int) {
        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
    }
}
