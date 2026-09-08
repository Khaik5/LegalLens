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
import com.example.lagallens.presentation.feature.upload.contract.UploadSourceEffect
import com.example.lagallens.presentation.feature.upload.contract.UploadSourceEvent
import com.example.lagallens.presentation.feature.upload.contract.UploadSourceUiState
import com.example.lagallens.presentation.feature.upload.viewmodel.UploadSourceViewModel
import kotlinx.coroutines.launch

class UploadSourceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUploadSourceBinding
    private val viewModel by viewModels<UploadSourceViewModel>()
    private val documentPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.onEvent(UploadSourceEvent.FileSelected)
    }
    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.onEvent(UploadSourceEvent.FileSelected)
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
        binding = ActivityUploadSourceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.uploadSourceRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        bindEvents()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.effects.collect(::handleEffect) }
            }
        }
    }

    private fun bindEvents() {
        binding.uploadSourceBack.setOnClickListener {
            viewModel.onEvent(UploadSourceEvent.BackClicked)
        }
        binding.uploadSourcePdf.setOnClickListener {
            viewModel.onEvent(UploadSourceEvent.PdfClicked)
        }
        binding.uploadSourceDocx.setOnClickListener {
            viewModel.onEvent(UploadSourceEvent.DocxClicked)
        }
        binding.uploadSourceGallery.setOnClickListener {
            viewModel.onEvent(UploadSourceEvent.GalleryClicked)
        }
        binding.uploadSourceCamera.setOnClickListener {
            viewModel.onEvent(UploadSourceEvent.CameraClicked)
        }
    }

    private fun render(state: UploadSourceUiState) {
        binding.uploadSourcePdf.isEnabled = state.isSourceSelectionAvailable
        binding.uploadSourceDocx.isEnabled = state.isSourceSelectionAvailable
        binding.uploadSourceGallery.isEnabled = state.isSourceSelectionAvailable
        binding.uploadSourceCamera.isEnabled = state.isSourceSelectionAvailable
    }

    private fun handleEffect(effect: UploadSourceEffect) {
        when (effect) {
            UploadSourceEffect.NavigateBack -> finish()
            UploadSourceEffect.NavigateToCameraPermission -> startActivity(
                CameraPermissionActivity.createIntent(this)
            )
            is UploadSourceEffect.OpenDocumentPicker -> documentPicker.launch(effect.mimeTypes)
            UploadSourceEffect.OpenImagePicker -> imagePicker.launch("image/*")
            UploadSourceEffect.ShowFileSelected -> showMessage(R.string.upload_source_selected)
        }
    }

    private fun showMessage(messageRes: Int) {
        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
    }
}
