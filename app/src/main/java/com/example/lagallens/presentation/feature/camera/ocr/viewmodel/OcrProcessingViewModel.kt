package com.example.lagallens.presentation.feature.camera.ocr.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.camera.ocr.contract.OcrProcessingUiEffect
import com.example.lagallens.presentation.feature.camera.ocr.contract.OcrProcessingUiEvent
import com.example.lagallens.presentation.feature.camera.ocr.contract.OcrProcessingUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OcrProcessingViewModel(
    private val processingStageDurationMillis: Long = PROCESSING_STAGE_DURATION_MILLIS
) : ViewModel() {
    private val _uiState = MutableStateFlow(OcrProcessingUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<OcrProcessingUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    private var hasStartedLocalPreview = false

    fun onEvent(event: OcrProcessingUiEvent) {
        when (event) {
            is OcrProcessingUiEvent.ScreenOpened -> startLocalPreview(event.shouldSimulateFailure)
        }
    }

    fun setDocumentMetadata(documentName: String, documentDetails: String) {
        _uiState.update {
            it.copy(
                documentName = documentName,
                documentDetails = documentDetails
            )
        }
    }

    private fun startLocalPreview(shouldSimulateFailure: Boolean) {
        if (hasStartedLocalPreview) return
        hasStartedLocalPreview = true
        viewModelScope.launch {
            delay(processingStageDurationMillis)
            val state = _uiState.value
            if (shouldSimulateFailure) {
                _uiEffect.emit(
                    OcrProcessingUiEffect.NavigateToOcrFailure(
                        documentName = state.documentName,
                        documentDetails = state.documentDetails
                    )
                )
                return@launch
            }
            (3..4).forEach { activeStepId ->
                _uiState.update { state -> state.withActiveStep(activeStepId) }
                delay(processingStageDurationMillis)
            }
            _uiEffect.emit(
                OcrProcessingUiEffect.NavigateToOcrResult(
                    documentName = state.documentName,
                    documentDetails = state.documentDetails
                )
            )
        }
    }

    private companion object {
        const val PROCESSING_STAGE_DURATION_MILLIS = 900L
    }
}
