package com.example.lagallens.presentation.feature.upload.ui

import android.graphics.Color
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.format.Formatter
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
import com.example.lagallens.presentation.feature.upload.contract.UploadSourceEffect
import com.example.lagallens.presentation.feature.upload.contract.UploadSourceEvent
import com.example.lagallens.presentation.feature.upload.contract.UploadSourceUiState
import com.example.lagallens.presentation.feature.upload.progress.ui.UploadProgressActivity
import com.example.lagallens.presentation.feature.upload.viewmodel.UploadSourceViewModel
import kotlinx.coroutines.launch

class UploadSourceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUploadSourceBinding
    private val viewModel by viewModels<UploadSourceViewModel>()
    private val documentPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(::openUploadProgress)
    }
    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(::openUploadProgress)
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
            is UploadSourceEffect.OpenDocumentPicker -> documentPicker.launch(effect.mimeTypes)
            UploadSourceEffect.OpenImagePicker -> imagePicker.launch("image/*")
            UploadSourceEffect.ShowFileSelected -> showMessage(R.string.upload_source_selected)
            UploadSourceEffect.ShowCameraUnavailable -> showMessage(R.string.upload_camera_unavailable)
        }
    }

    private fun showMessage(messageRes: Int) {
        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
    }

    private fun openUploadProgress(uri: android.net.Uri) {
        val fileMetadata = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                val fileName = if (nameColumn >= 0) cursor.getString(nameColumn) else null
                val fileSize = if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) cursor.getLong(sizeColumn) else null
                fileName to fileSize
            } else {
                null
            }
        }
        val fileName = fileMetadata?.first ?: getString(R.string.upload_source_selected_file)
        val fileSize = fileMetadata?.second?.let { Formatter.formatShortFileSize(this, it) }
        val fileType = contentResolver.getType(uri)?.let(::formatFileType).orEmpty()
        val fileDetails = listOfNotNull(fileSize, fileType.takeIf { it.isNotBlank() }).joinToString(" · ")
        startActivity(UploadProgressActivity.newIntent(this, fileName, fileDetails))
    }

    private fun formatFileType(mimeType: String): String {
        return when (mimeType) {
            "application/pdf" -> getString(R.string.upload_file_type_pdf)
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> getString(R.string.upload_file_type_docx)
            else -> mimeType.substringAfterLast('/').uppercase()
        }
    }
}
