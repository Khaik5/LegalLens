package com.example.lagallens.presentation.feature.camera.capture.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.presentation.feature.camera.core.DocumentDetection
import com.example.lagallens.presentation.feature.camera.core.DocumentQualityResult
import com.example.lagallens.presentation.feature.camera.core.QualityDecision
import com.example.lagallens.presentation.feature.camera.core.ScanPage
import com.example.lagallens.presentation.feature.camera.capture.contract.CameraCaptureEffect
import com.example.lagallens.presentation.feature.camera.capture.contract.CameraCaptureEvent
import com.example.lagallens.presentation.feature.camera.capture.contract.CameraCaptureUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CameraCaptureViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CameraCaptureUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<CameraCaptureEffect>()
    val effects = _effects.asSharedFlow()
    private var pendingWarningPage: ScanPage? = null
    private var consecutiveReadyFrames = 0

    fun onEvent(event: CameraCaptureEvent) {
        when (event) {
            CameraCaptureEvent.CloseClicked -> emitEffect(CameraCaptureEffect.NavigateBack)
            CameraCaptureEvent.FlashClicked -> _uiState.update { it.copy(isFlashEnabled = !it.isFlashEnabled) }
            CameraCaptureEvent.SettingsClicked -> _uiState.update {
                it.copy(isAutoCaptureEnabled = !it.isAutoCaptureEnabled)
            }
            CameraCaptureEvent.LibraryClicked -> emitEffect(CameraCaptureEffect.OpenGallery)
            CameraCaptureEvent.ShutterClicked -> requestCapture()
            is CameraCaptureEvent.DetectionUpdated -> {
                _uiState.update { it.copy(detection = event.detection) }
                consecutiveReadyFrames = if (event.detection is DocumentDetection.Ready) {
                    consecutiveReadyFrames + 1
                } else {
                    0
                }
                if (
                    event.detection is DocumentDetection.Ready &&
                    _uiState.value.isAutoCaptureEnabled &&
                    consecutiveReadyFrames >= 12
                ) {
                    requestCapture()
                    consecutiveReadyFrames = 0
                }
            }
            is CameraCaptureEvent.PhotoQualityChecked -> {
                _uiState.update { it.copy(isCapturing = false) }
                handleQualityResult(event.page, event.qualityResult)
            }
            CameraCaptureEvent.CaptureFailed -> _uiState.update { it.copy(isCapturing = false) }
            is CameraCaptureEvent.GalleryPagesQualityChecked -> {
                event.pages.forEach { (page, qualityResult) -> handleQualityResult(page, qualityResult) }
            }
            CameraCaptureEvent.KeepWarningPage -> {
                pendingWarningPage?.let { addPage(it.copy(hasQualityWarning = true)) }
                pendingWarningPage = null
            }
            CameraCaptureEvent.DiscardWarningPage -> {
                pendingWarningPage = null
            }
            is CameraCaptureEvent.PagesUpdated -> {
                _uiState.update { it.copy(pages = event.pages) }
            }
            CameraCaptureEvent.ContinueClicked -> {
                if (_uiState.value.pages.isEmpty()) {
                    emitEffect(CameraCaptureEffect.ShowMessage(R.string.camera_capture_add_page_first))
                } else {
                    emitEffect(CameraCaptureEffect.NavigateToPageManager(_uiState.value.pages))
                }
            }
        }
    }

    private fun requestCapture() {
        val state = _uiState.value
        if (state.isCapturing || state.detection !is DocumentDetection.Ready) return
        _uiState.update { it.copy(isCapturing = true) }
        emitEffect(CameraCaptureEffect.CapturePhoto)
    }

    private fun handleQualityResult(page: ScanPage, qualityResult: DocumentQualityResult) {
        when (qualityResult.decision) {
            QualityDecision.ACCEPTED -> addPage(page)
            QualityDecision.WARNING -> {
                pendingWarningPage = page
                emitEffect(CameraCaptureEffect.ShowQualityResult(page, qualityResult))
            }
            QualityDecision.REJECTED -> {
                emitEffect(CameraCaptureEffect.ShowQualityResult(page, qualityResult))
            }
        }
    }

    private fun addPage(page: ScanPage) {
        _uiState.update { it.copy(pages = it.pages + page) }
        emitEffect(CameraCaptureEffect.ShowMessage(R.string.camera_capture_photo_ready))
    }

    private fun emitEffect(effect: CameraCaptureEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }
}
