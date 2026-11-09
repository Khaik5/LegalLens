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
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import com.example.lagallens.presentation.feature.camera.permission.ui.CameraPermissionActivity
import com.example.lagallens.presentation.feature.upload.contract.UploadSourceUiEffect
import com.example.lagallens.presentation.feature.upload.contract.UploadSourceUiEvent
import com.example.lagallens.presentation.feature.upload.contract.UploadSourceUiState
import com.example.lagallens.presentation.feature.upload.progress.ui.UploadProgressActivity
import com.example.lagallens.presentation.feature.upload.viewmodel.UploadSourceViewModel
import com.example.lagallens.presentation.feature.upload.UploadFileValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UploadSourceActivity : com.example.lagallens.presentation.feature.ocr.OcrSessionActivity() {
    private val binding by lazy { ActivityUploadSourceBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<UploadSourceViewModel>()
    private val documentPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(::openUploadProgress)
    }
    private val imagePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(::openUploadProgress)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!intent.hasExtra(com.example.lagallens.presentation.feature.ocr.OcrSession.EXTRA_OWNER))
            intent.putExtra(com.example.lagallens.presentation.feature.ocr.OcrSession.EXTRA_OWNER, (application as com.example.lagallens.LegalLensApplication).currentSessionKey())
        super.onCreate(savedInstanceState)
        if (isFinishing || !isOcrSessionValid()) { finish(); return }
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
        listOf(binding.uploadSourceBack, binding.uploadSourcePdf, binding.uploadSourceDocx, binding.uploadSourceGallery, binding.uploadSourceCamera).forEach { it.bindPressFeedback() }
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
        if (!isOcrSessionValid()) return
        when (effect) {
            UploadSourceUiEffect.NavigateBack -> finish()
            UploadSourceUiEffect.NavigateToCameraPermission -> startActivity(
                CameraPermissionActivity.createIntent(this)
            )
            is UploadSourceUiEffect.OpenDocumentPicker -> documentPicker.launch(effect.mimeTypes)
            UploadSourceUiEffect.OpenImagePicker -> imagePicker.launch(arrayOf("image/*"))
            UploadSourceUiEffect.ShowFileSelected -> showMessage(R.string.upload_source_selected)
        }
    }

    private fun showMessage(messageRes: Int) {
        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
    }

    private fun openUploadProgress(uri: android.net.Uri) {
        if (!isOcrSessionValid()) return
        runCatching {
            contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val fileMetadata = try { contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                val fileName = if (nameColumn >= 0) cursor.getString(nameColumn) else null
                val fileSize = if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) cursor.getLong(sizeColumn) else null
                fileName to fileSize
            } else {
                null
            }
        } } catch (_: Exception) { showMessage(R.string.upload_source_file_unreadable); return }
        val fileName = fileMetadata?.first ?: getString(R.string.upload_source_selected_file)
        val byteCount = fileMetadata?.second
        val mimeType = try { contentResolver.getType(uri).orEmpty() }
            catch (_: Exception) { showMessage(R.string.upload_source_file_unreadable); return }
        if (!UploadFileValidator.isSupported(mimeType)) {
            showMessage(R.string.upload_source_unsupported_file)
            return
        }
        lifecycleScope.launch {
            // Some providers report no size, so the limit is enforced on the stream itself.
            val measuredBytes = withContext(Dispatchers.IO) {
                try { contentResolver.openInputStream(uri)?.use { byteCount ?: UploadFileValidator.countBytes(it) } }
                catch (_: Exception) { null }
            }
            if (!isOcrSessionValid()) return@launch
            when {
                measuredBytes == null -> showMessage(R.string.upload_source_file_unreadable)
                measuredBytes > UploadFileValidator.MAX_BYTES -> showMessage(R.string.upload_source_file_too_large)
                measuredBytes == 0L -> showMessage(R.string.upload_source_file_empty)
                else -> {
                    val fileSize = Formatter.formatShortFileSize(this@UploadSourceActivity, measuredBytes)
                    val fileType = mimeType.takeIf(String::isNotBlank)?.let(::formatFileType).orEmpty()
                    val fileDetails = listOf(fileType, fileSize).filter(String::isNotBlank).joinToString(" · ")
                    startActivity(UploadProgressActivity.newIntent(this@UploadSourceActivity, fileName, fileDetails, uri.toString(), java.util.UUID.randomUUID().toString(), mimeType))
                }
            }
        }
    }

    private fun formatFileType(mimeType: String): String {
        return when (mimeType) {
            "application/pdf" -> getString(R.string.upload_file_type_pdf)
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> getString(R.string.upload_file_type_docx)
            else -> mimeType.substringAfterLast('/').uppercase()
        }
    }
}
