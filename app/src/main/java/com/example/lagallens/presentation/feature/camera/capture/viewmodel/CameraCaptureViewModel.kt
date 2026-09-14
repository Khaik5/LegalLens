package com.example.lagallens.presentation.feature.camera.capture.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.domain.model.camera.DocumentDetection
import com.example.lagallens.domain.model.camera.DocumentQualityResult
import com.example.lagallens.domain.model.camera.QualityDecision
import com.example.lagallens.domain.model.camera.ScanPage
import com.example.lagallens.presentation.feature.camera.capture.contract.CameraCaptureUiEffect
import com.example.lagallens.presentation.feature.camera.capture.contract.CameraCaptureUiEvent
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

    private val _uiEffect = MutableSharedFlow<CameraCaptureUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    private var pendingWarningPage: ScanPage? = null
    private var consecutiveReadyFrames = 0

    fun onEvent(event: CameraCaptureUiEvent) {
        when (event) {
            CameraCaptureUiEvent.CloseClicked -> emitEffect(CameraCaptureUiEffect.NavigateBack)
            CameraCaptureUiEvent.FlashClicked -> _uiState.update { it.copy(isFlashEnabled = !it.isFlashEnabled) }
            CameraCaptureUiEvent.SettingsClicked -> _uiState.update {
                it.copy(isAutoCaptureEnabled = !it.isAutoCaptureEnabled)
            }
            CameraCaptureUiEvent.LibraryClicked -> emitEffect(CameraCaptureUiEffect.OpenGallery)
            CameraCaptureUiEvent.ShutterClicked -> requestCapture()
            is CameraCaptureUiEvent.DetectionUpdated -> {
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
            is CameraCaptureUiEvent.PhotoQualityChecked -> {
                _uiState.update { it.copy(isCapturing = false) }
                handleQualityResult(event.page, event.qualityResult)
            }
            CameraCaptureUiEvent.CaptureFailed -> _uiState.update { it.copy(isCapturing = false) }
            is CameraCaptureUiEvent.GalleryPagesQualityChecked -> {
                event.pages.forEach { (page, qualityResult) -> handleQualityResult(page, qualityResult) }
            }
            CameraCaptureUiEvent.KeepWarningPage -> {
                pendingWarningPage?.let { addPage(it.copy(hasQualityWarning = true)) }
                pendingWarningPage = null
            }
            CameraCaptureUiEvent.DiscardWarningPage -> {
                pendingWarningPage = null
            }
            is CameraCaptureUiEvent.PagesUpdated -> {
                _uiState.update { it.copy(pages = event.pages) }
            }
            CameraCaptureUiEvent.ContinueClicked -> {
                if (_uiState.value.pages.isEmpty()) {
                    emitEffect(CameraCaptureUiEffect.ShowMessage(R.string.camera_capture_add_page_first))
                } else {
                    emitEffect(CameraCaptureUiEffect.NavigateToPageManager(_uiState.value.pages))
                }
            }
        }
    }

    private fun requestCapture() {
        val state = _uiState.value
        if (state.isCapturing || state.detection !is DocumentDetection.Ready) return
        _uiState.update { it.copy(isCapturing = true) }
        emitEffect(CameraCaptureUiEffect.CapturePhoto)
    }

    private fun handleQualityResult(page: ScanPage, qualityResult: DocumentQualityResult) {
        when (qualityResult.decision) {
            QualityDecision.ACCEPTED -> addPage(page)
            QualityDecision.WARNING -> {
                pendingWarningPage = page
                emitEffect(CameraCaptureUiEffect.ShowQualityResult(page, qualityResult))
            }
            QualityDecision.REJECTED -> {
                emitEffect(CameraCaptureUiEffect.ShowQualityResult(page, qualityResult))
            }
        }
    }

    private fun addPage(page: ScanPage) {
        _uiState.update { it.copy(pages = it.pages + page) }
        emitEffect(CameraCaptureUiEffect.ShowMessage(R.string.camera_capture_photo_ready))
    }

    private fun emitEffect(effect: CameraCaptureUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }
}
