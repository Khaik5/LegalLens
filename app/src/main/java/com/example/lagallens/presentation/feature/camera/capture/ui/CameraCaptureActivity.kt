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
import com.example.lagallens.presentation.feature.camera.capture.contract.CameraCaptureEffect
import com.example.lagallens.presentation.feature.camera.capture.contract.CameraCaptureEvent
import com.example.lagallens.presentation.feature.camera.capture.contract.CameraCaptureUiState
import com.example.lagallens.presentation.feature.camera.capture.viewmodel.CameraCaptureViewModel
import com.example.lagallens.presentation.feature.camera.core.DocumentDetection
import com.example.lagallens.presentation.feature.camera.core.DocumentVisionAnalyzer
import com.example.lagallens.presentation.feature.camera.core.QualityDecision
import com.example.lagallens.presentation.feature.camera.core.ScanPage
import com.example.lagallens.presentation.feature.camera.pages.ui.CameraPageManagerActivity
import kotlinx.coroutines.launch
import org.opencv.android.OpenCVLoader
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraCaptureActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraCaptureBinding
    private val viewModel by viewModels<CameraCaptureViewModel>()
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private lateinit var imageCapture: ImageCapture
    private var camera: Camera? = null
    private var openCvAvailable = false
    private var previousReadyDetection: DocumentDetection.Ready? = null
    private var shakeRisk = false
    private val galleryPicker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) importGalleryPages(uris.map { it.toString() })
    }
    private val pageManager = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val paths = result.data?.getStringArrayListExtra(CameraPageManagerActivity.EXTRA_PAGE_PATHS)
        val warningPaths = result.data?.getStringArrayListExtra(CameraPageManagerActivity.EXTRA_WARNING_PATHS)
        if (paths != null) {
            val warningSet = warningPaths.orEmpty().toSet()
            viewModel.onEvent(CameraCaptureEvent.PagesUpdated(paths.map { ScanPage(it, it in warningSet) }))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        binding = ActivityCameraCaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.cameraCaptureRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        openCvAvailable = OpenCVLoader.initLocal()
        bindEvents()
        startCamera()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.effects.collect(::handleEffect) }
            }
        }
    }

    override fun onDestroy() {
        cameraExecutor.shutdown()
        super.onDestroy()
    }

    private fun bindEvents() {
        binding.cameraCaptureClose.setOnClickListener { viewModel.onEvent(CameraCaptureEvent.CloseClicked) }
        binding.cameraCaptureFlash.setOnClickListener { viewModel.onEvent(CameraCaptureEvent.FlashClicked) }
        binding.cameraCaptureSettings.setOnClickListener { viewModel.onEvent(CameraCaptureEvent.SettingsClicked) }
        binding.cameraCaptureLibrary.setOnClickListener { viewModel.onEvent(CameraCaptureEvent.LibraryClicked) }
        binding.cameraCaptureShutter.setOnClickListener { viewModel.onEvent(CameraCaptureEvent.ShutterClicked) }
        binding.cameraCaptureContinue.setOnClickListener { viewModel.onEvent(CameraCaptureEvent.ContinueClicked) }
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = runCatching { future.get() }.getOrNull() ?: run {
                viewModel.onEvent(CameraCaptureEvent.DetectionUpdated(DocumentDetection.Unavailable))
                return@addListener
            }
            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.surfaceProvider = binding.cameraCapturePreview.surfaceProvider
            }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(cameraExecutor) { image ->
                val detection = if (openCvAvailable) DocumentVisionAnalyzer.detect(image) else DocumentDetection.Unavailable
                image.close()
                updateDetection(detection)
            }
            camera = runCatching {
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture, analysis)
            }.getOrNull()
            if (camera == null) viewModel.onEvent(CameraCaptureEvent.DetectionUpdated(DocumentDetection.Unavailable))
        }, ContextCompat.getMainExecutor(this))
    }

    private fun updateDetection(detection: DocumentDetection) {
        if (detection is DocumentDetection.Ready && previousReadyDetection != null) {
            val previous = previousReadyDetection ?: return
            shakeRisk = kotlin.math.abs(previous.centerX - detection.centerX) > 0.04 ||
                kotlin.math.abs(previous.centerY - detection.centerY) > 0.04 ||
                kotlin.math.abs(previous.areaRatio - detection.areaRatio) > 0.08
        } else if (detection !is DocumentDetection.Ready) {
            shakeRisk = false
        }
        previousReadyDetection = detection as? DocumentDetection.Ready
        val visibleDetection = if (detection is DocumentDetection.Ready && shakeRisk) {
            DocumentDetection.Unstable
        } else {
            detection
        }
        runOnUiThread { viewModel.onEvent(CameraCaptureEvent.DetectionUpdated(visibleDetection)) }
    }

    private fun render(state: CameraCaptureUiState) {
        camera?.cameraControl?.enableTorch(state.isFlashEnabled)
        binding.cameraCaptureFlash.isSelected = state.isFlashEnabled
        binding.cameraCaptureCount.text = state.pages.size.toString()
        binding.cameraCaptureShutter.isEnabled = state.detection is DocumentDetection.Ready && !state.isCapturing
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

    private fun handleEffect(effect: CameraCaptureEffect) {
        when (effect) {
            CameraCaptureEffect.NavigateBack -> finish()
            CameraCaptureEffect.CapturePhoto -> capturePhoto()
            CameraCaptureEffect.OpenGallery -> galleryPicker.launch(arrayOf("image/*"))
            is CameraCaptureEffect.NavigateToPageManager -> pageManager.launch(
                CameraPageManagerActivity.createIntent(this, effect.pages)
            )
            is CameraCaptureEffect.ShowQualityResult -> showQualityResult(effect)
            is CameraCaptureEffect.ShowMessage -> Toast.makeText(this, effect.messageRes, Toast.LENGTH_SHORT).show()
        }
    }

    private fun capturePhoto() {
        val outputFile = createSessionFile()
        val options = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        imageCapture.takePicture(options, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val quality = DocumentVisionAnalyzer.checkQuality(outputFile, shakeRisk)
                runOnUiThread {
                    viewModel.onEvent(CameraCaptureEvent.PhotoQualityChecked(ScanPage(outputFile.path), quality))
                }
            }

            override fun onError(exception: ImageCaptureException) {
                outputFile.delete()
                runOnUiThread {
                    Toast.makeText(this@CameraCaptureActivity, R.string.camera_capture_failed, Toast.LENGTH_SHORT).show()
                    viewModel.onEvent(CameraCaptureEvent.CaptureFailed)
                    viewModel.onEvent(CameraCaptureEvent.DetectionUpdated(DocumentDetection.NotFound))
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
            runOnUiThread { viewModel.onEvent(CameraCaptureEvent.GalleryPagesQualityChecked(results)) }
        }
    }

    private fun showQualityResult(effect: CameraCaptureEffect.ShowQualityResult) {
        val messages = effect.qualityResult.issues.joinToString("\n") { getString(it.messageRes) }
        val isWarning = effect.qualityResult.decision == QualityDecision.WARNING
        AlertDialog.Builder(this)
            .setTitle(if (isWarning) R.string.camera_quality_warning_title else R.string.camera_quality_retake_title)
            .setMessage(messages)
            .setNegativeButton(R.string.camera_quality_retake) { _, _ ->
                File(effect.page.path).delete()
                if (isWarning) viewModel.onEvent(CameraCaptureEvent.DiscardWarningPage)
            }
            .apply {
                if (isWarning) {
                    setPositiveButton(R.string.camera_quality_keep) { _, _ ->
                        viewModel.onEvent(CameraCaptureEvent.KeepWarningPage)
                    }
                } else {
                    setPositiveButton(R.string.camera_quality_ok, null)
                }
            }
            .show()
    }

    private fun createSessionFile(): File {
        val directory = File(cacheDir, "scan-session").apply { mkdirs() }
        return File.createTempFile("scan_", ".jpg", directory)
    }

    companion object {
        fun createIntent(context: Context): Intent = Intent(context, CameraCaptureActivity::class.java)
    }
}
