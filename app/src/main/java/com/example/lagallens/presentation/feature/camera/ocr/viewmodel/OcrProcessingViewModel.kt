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

class OcrProcessingViewModel : ViewModel() {
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
            delay(LOCAL_PREVIEW_DURATION_MILLIS)
            val state = _uiState.value
            _uiEffect.emit(
                if (shouldSimulateFailure) {
                    OcrProcessingUiEffect.NavigateToOcrFailure(
                        documentName = state.documentName,
                        documentDetails = state.documentDetails
                    )
                } else {
                    OcrProcessingUiEffect.NavigateToOcrResult(
                        documentName = state.documentName,
                        documentDetails = state.documentDetails
                    )
                }
            )
        }
    }

    private companion object {
        const val LOCAL_PREVIEW_DURATION_MILLIS = 2_000L
    }
}
