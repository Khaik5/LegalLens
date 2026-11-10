package com.example.lagallens.presentation.feature.camera.capture.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.UseCaseGroup
import androidx.camera.view.PreviewView
import androidx.core.view.doOnLayout
import com.example.lagallens.core.camera.DetectionStabilizer
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityCameraCaptureBinding
import com.example.lagallens.presentation.feature.camera.capture.contract.CameraCaptureUiEffect
import com.example.lagallens.presentation.feature.camera.capture.contract.CameraCaptureUiEvent
import com.example.lagallens.presentation.feature.camera.capture.contract.PendingQualityCheck
import com.example.lagallens.presentation.feature.camera.capture.contract.CameraCaptureUiState
import com.example.lagallens.presentation.feature.camera.capture.viewmodel.CameraCaptureViewModel
import com.example.lagallens.domain.model.camera.DocumentDetection
import com.example.lagallens.domain.model.camera.QualityIssue
import com.example.lagallens.core.camera.DocumentVisionAnalyzer
import com.example.lagallens.domain.model.camera.QualityDecision
import com.example.lagallens.domain.model.camera.ScanPage
import com.example.lagallens.presentation.feature.camera.pages.ui.CameraPageManagerActivity
import kotlinx.coroutines.launch
import org.opencv.android.OpenCVLoader
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraCaptureActivity : com.example.lagallens.presentation.feature.ocr.OcrSessionActivity() {
    private val binding by lazy { ActivityCameraCaptureBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<CameraCaptureViewModel>()
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var qualityDialog: AlertDialog? = null
    private var shownQualityPath: String? = null
    private var openCvAvailable = false
    private val detectionStabilizer = DetectionStabilizer()
    @Volatile private var shakeRisk = false
    private val galleryPicker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) importGalleryPages(uris.map { it.toString() })
    }
    private val pageManager = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val paths = result.data?.getStringArrayListExtra(CameraPageManagerActivity.EXTRA_PAGE_PATHS)
        val warningPaths = result.data?.getStringArrayListExtra(CameraPageManagerActivity.EXTRA_WARNING_PATHS)
        if (paths != null) {
            val warningSet = warningPaths.orEmpty().toSet()
            viewModel.onEvent(CameraCaptureUiEvent.PagesUpdated(paths.map { ScanPage(it, it in warningSet) }))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isFinishing || !isOcrSessionValid()) { finish(); return }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setContentView(binding.root)
        applyWindowInsets()
        openCvAvailable = OpenCVLoader.initLocal()
        bindView()
        startCamera()
        collectViewModel()
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.cameraCaptureRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
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

    override fun onDestroy() {
        qualityDialog?.dismiss()
        cameraExecutor.shutdown()
        super.onDestroy()
    }

    private fun bindView() {
        binding.cameraCaptureClose.setOnClickListener { viewModel.onEvent(CameraCaptureUiEvent.CloseClicked) }
        binding.cameraCaptureFlash.setOnClickListener { viewModel.onEvent(CameraCaptureUiEvent.FlashClicked) }
        binding.cameraCaptureSettings.setOnClickListener { viewModel.onEvent(CameraCaptureUiEvent.SettingsClicked) }
        binding.cameraCaptureLibrary.setOnClickListener { viewModel.onEvent(CameraCaptureUiEvent.LibraryClicked) }
        binding.cameraCaptureShutter.setOnClickListener { viewModel.onEvent(CameraCaptureUiEvent.ShutterClicked) }
        binding.cameraCaptureContinue.setOnClickListener { viewModel.onEvent(CameraCaptureUiEvent.ContinueClicked) }
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = runCatching { future.get() }.getOrNull() ?: run {
                viewModel.onEvent(CameraCaptureUiEvent.DetectionUpdated(DocumentDetection.Unavailable))
                return@addListener
            }
            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.surfaceProvider = binding.cameraCapturePreview.surfaceProvider
            }
            // Documents need legible small text: full sensor resolution and the camera's quality pipeline.
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                        .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                        .build()
                )
                .setJpegQuality(95)
                .build()
            imageCapture = capture
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                        .build()
                )
                .build()
            analysis.setAnalyzer(cameraExecutor) { image ->
                val detection = if (openCvAvailable) DocumentVisionAnalyzer.detect(image) else DocumentDetection.Unavailable
                image.close()
                updateDetection(detection)
            }
            // The preview crops the sensor image to fill the screen. Sharing its ViewPort makes the saved photo and
            // the analysed frames cover exactly what the user framed, so "page too small" matches the screen.
            binding.cameraCapturePreview.doOnLayout { previewView ->
                if (isDestroyed) return@doOnLayout
                val useCases = UseCaseGroup.Builder()
                    .addUseCase(preview)
                    .addUseCase(capture)
                    .addUseCase(analysis)
                    .apply { (previewView as PreviewView).viewPort?.let(::setViewPort) }
                    .build()
                camera = runCatching {
                    provider.unbindAll()
                    provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, useCases)
                }.getOrNull()
                if (camera == null) viewModel.onEvent(CameraCaptureUiEvent.DetectionUpdated(DocumentDetection.Unavailable))
            }
        }, ContextCompat.getMainExecutor(this))
    }

    /** Runs on [cameraExecutor], the only thread that touches [detectionStabilizer]. */
    private fun updateDetection(detection: DocumentDetection) {
        val visibleDetection = detectionStabilizer.update(detection)
        shakeRisk = detectionStabilizer.isShaking
        runOnUiThread { viewModel.onEvent(CameraCaptureUiEvent.DetectionUpdated(visibleDetection)) }
    }

    private fun render(state: CameraCaptureUiState) {
        camera?.cameraControl?.enableTorch(state.isFlashEnabled)
        binding.cameraCaptureFlash.isSelected = state.isFlashEnabled
        binding.cameraCaptureCount.text = state.pages.size.toString()
        showNextQualityCheck(state.pendingQualityChecks.firstOrNull())
        binding.cameraCaptureShutter.isEnabled = !state.isCapturing && imageCapture != null
        binding.cameraCaptureShutter.alpha = if (binding.cameraCaptureShutter.isEnabled) 1f else 0.45f
        binding.cameraCaptureScanFrame.setBackgroundResource(
            when (state.detection) {
                is DocumentDetection.Ready -> R.drawable.bg_camera_scan_frame_ready
                is DocumentDetection.Partial -> R.drawable.bg_camera_scan_frame_warning
                else -> R.drawable.bg_camera_scan_frame_hidden
            }
        )
        binding.cameraCaptureInstruction.setText(
            when (state.detection) {
                is DocumentDetection.Ready -> if (state.isAutoCaptureEnabled) {
                    R.string.camera_capture_document_ready_auto
                } else {
                    R.string.camera_capture_document_ready_manual
                }
                is DocumentDetection.Partial -> R.string.camera_capture_document_partial
                DocumentDetection.Unstable -> R.string.camera_capture_document_unstable
                DocumentDetection.Unavailable -> R.string.camera_capture_detection_unavailable
                DocumentDetection.NotFound -> R.string.camera_capture_document_not_found
            }
        )
        binding.cameraCaptureChip.setText(
            if (state.isAutoCaptureEnabled) R.string.camera_capture_auto_align else R.string.camera_capture_manual_mode
        )
    }

    private fun handleEffect(effect: CameraCaptureUiEffect) {
        when (effect) {
            CameraCaptureUiEffect.NavigateBack -> finish()
            CameraCaptureUiEffect.CapturePhoto -> capturePhoto()
            CameraCaptureUiEffect.OpenGallery -> galleryPicker.launch(arrayOf("image/*"))
            is CameraCaptureUiEffect.NavigateToPageManager -> pageManager.launch(
                CameraPageManagerActivity.createIntent(this, effect.pages)
            )
            is CameraCaptureUiEffect.ShowMessage -> Toast.makeText(this, effect.messageRes, Toast.LENGTH_SHORT).show()
        }
    }

    private fun capturePhoto() {
        val capture = imageCapture ?: return viewModel.onEvent(CameraCaptureUiEvent.CaptureFailed)
        val outputFile = createSessionFile()
        val options = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        // Read on the main thread at the shutter moment: a stable green frame already confirmed the framing.
        val framedByPreview = viewModel.uiState.value.detection is DocumentDetection.Ready
        val shakenAtShutter = shakeRisk
        capture.takePicture(options, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val quality = DocumentVisionAnalyzer.checkQuality(outputFile, shakenAtShutter, framedByPreview)
                runOnUiThread {
                    viewModel.onEvent(CameraCaptureUiEvent.PhotoQualityChecked(ScanPage(outputFile.path), quality))
                }
            }

            override fun onError(exception: ImageCaptureException) {
                outputFile.delete()
                runOnUiThread {
                    Toast.makeText(this@CameraCaptureActivity, R.string.camera_capture_failed, Toast.LENGTH_SHORT).show()
                    viewModel.onEvent(CameraCaptureUiEvent.CaptureFailed)
                    viewModel.onEvent(CameraCaptureUiEvent.DetectionUpdated(DocumentDetection.NotFound))
                }
            }
        })
    }

    private fun importGalleryPages(uriValues: List<String>) {
        cameraExecutor.execute {
            val results = uriValues.mapNotNull { value ->
                val file = createSessionFile()
                val copied = runCatching {
                    contentResolver.openInputStream(android.net.Uri.parse(value))?.use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                        true
                    } ?: false
                }.getOrDefault(false)
                if (copied) ScanPage(file.path) to DocumentVisionAnalyzer.checkQuality(file, false) else {
                    file.delete()
                    null
                }
            }
            runOnUiThread { viewModel.onEvent(CameraCaptureUiEvent.GalleryPagesQualityChecked(results)) }
        }
    }

    private fun showNextQualityCheck(check: PendingQualityCheck?) {
        if (check == null || check.page.path == shownQualityPath) return
        shownQualityPath = check.page.path
        val messages = check.qualityResult.issues.joinToString("\n") { getString(it.messageResId()) }
        val isWarning = check.qualityResult.decision == QualityDecision.WARNING
        fun discard() {
            File(check.page.path).delete()
            viewModel.onEvent(CameraCaptureUiEvent.DiscardPage(check.page.path))
        }
        qualityDialog = AlertDialog.Builder(this)
            .setTitle(if (isWarning) R.string.camera_quality_warning_title else R.string.camera_quality_retake_title)
            .setMessage(messages)
            .setCancelable(false)
            .setNegativeButton(R.string.camera_quality_retake) { _, _ -> discard() }
            .apply {
                if (isWarning) setPositiveButton(R.string.camera_quality_keep) { _, _ ->
                    viewModel.onEvent(CameraCaptureUiEvent.KeepWarningPage(check.page.path))
                } else setPositiveButton(R.string.camera_quality_ok) { _, _ -> discard() }
            }
            .show()
    }

    private fun createSessionFile(): File {
        val directory = File(cacheDir, "scan-session").apply { mkdirs() }
        return File.createTempFile("scan_", ".jpg", directory)
    }

    private fun QualityIssue.messageResId(): Int = when (this) {
        QualityIssue.BLUR -> R.string.camera_quality_blur
        QualityIssue.EXPOSURE -> R.string.camera_quality_exposure
        QualityIssue.SHAKE -> R.string.camera_quality_shake
        QualityIssue.DOCUMENT_SMALL -> R.string.camera_quality_document_small
        QualityIssue.CROP_RISK -> R.string.camera_quality_crop_risk
        QualityIssue.DOCUMENT_NOT_FOUND -> R.string.camera_quality_document_not_found
    }

    companion object {
        fun createIntent(context: Context): Intent = com.example.lagallens.presentation.feature.ocr.OcrSession.stamp(context, Intent(context, CameraCaptureActivity::class.java))
    }
}
