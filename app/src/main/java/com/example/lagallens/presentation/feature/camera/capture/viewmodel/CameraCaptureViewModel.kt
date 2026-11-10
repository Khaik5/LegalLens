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
import com.example.lagallens.presentation.feature.camera.capture.contract.PendingQualityCheck
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CameraCaptureViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CameraCaptureUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<CameraCaptureUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()
    private var isAutoCaptureArmed = true
    private var consecutiveReadyFrames = 0

    fun onEvent(event: CameraCaptureUiEvent) {
        when (event) {
            CameraCaptureUiEvent.CloseClicked -> emitEffect(CameraCaptureUiEffect.NavigateBack)
            CameraCaptureUiEvent.FlashClicked -> _uiState.update { it.copy(isFlashEnabled = !it.isFlashEnabled) }
            CameraCaptureUiEvent.SettingsClicked -> _uiState.update {
                it.copy(isAutoCaptureEnabled = !it.isAutoCaptureEnabled)
            }
            CameraCaptureUiEvent.LibraryClicked -> emitEffect(CameraCaptureUiEffect.OpenGallery)
            // A manual shot is always allowed; the quality check afterwards warns when no document was found.
            CameraCaptureUiEvent.ShutterClicked -> requestCapture(requiresDetection = false)
            is CameraCaptureUiEvent.DetectionUpdated -> {
                _uiState.update { it.copy(detection = event.detection) }
                if (event.detection !is DocumentDetection.Ready) {
                    consecutiveReadyFrames = 0
                    isAutoCaptureArmed = true
                } else {
                    consecutiveReadyFrames += 1
                    val state = _uiState.value
                    if (state.isAutoCaptureEnabled && isAutoCaptureArmed && state.pendingQualityChecks.isEmpty() && consecutiveReadyFrames >= AUTO_CAPTURE_READY_FRAMES) {
                        requestCapture(requiresDetection = true)
                        consecutiveReadyFrames = 0
                        isAutoCaptureArmed = false
                    }
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
            is CameraCaptureUiEvent.KeepWarningPage -> {
                _uiState.value.pendingQualityChecks.firstOrNull { it.page.path == event.path }?.let { addPage(it.page.copy(hasQualityWarning = true)) }
                removePending(event.path)
            }
            is CameraCaptureUiEvent.DiscardPage -> removePending(event.path)
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

    private fun requestCapture(requiresDetection: Boolean) {
        val state = _uiState.value
        if (state.isCapturing || (requiresDetection && state.detection !is DocumentDetection.Ready)) return
        _uiState.update { it.copy(isCapturing = true) }
        emitEffect(CameraCaptureUiEffect.CapturePhoto)
    }

    private fun handleQualityResult(page: ScanPage, qualityResult: DocumentQualityResult) {
        // Framing hints on a readable photo only mark the page; the user is asked only about unreadable photos.
        if (qualityResult.decision == QualityDecision.ACCEPTED) {
            addPage(if (qualityResult.issues.isEmpty()) page else page.copy(hasQualityWarning = true))
        } else _uiState.update { it.copy(pendingQualityChecks = it.pendingQualityChecks + PendingQualityCheck(page, qualityResult)) }
    }

    private fun removePending(path: String) {
        _uiState.update { state -> state.copy(pendingQualityChecks = state.pendingQualityChecks.filterNot { it.page.path == path }) }
    }

    private fun addPage(page: ScanPage) {
        _uiState.update { it.copy(pages = it.pages + page) }
        emitEffect(CameraCaptureUiEffect.ShowMessage(R.string.camera_capture_photo_ready))
    }

    private fun emitEffect(effect: CameraCaptureUiEffect) {
        viewModelScope.launch { _uiEffect.send(effect) }
    }

    private companion object {
        const val AUTO_CAPTURE_READY_FRAMES = 12
    }
}
