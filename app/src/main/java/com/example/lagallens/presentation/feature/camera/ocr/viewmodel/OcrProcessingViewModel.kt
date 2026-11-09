package com.example.lagallens.presentation.feature.camera.ocr.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lagallens.domain.model.processing.DocumentUploadRequest
import com.example.lagallens.domain.model.processing.ProcessingState
import com.example.lagallens.domain.repository.DocumentUploadRepository
import com.example.lagallens.domain.repository.OcrRepository
import com.example.lagallens.presentation.feature.camera.ocr.contract.OcrProcessingUiEffect
import com.example.lagallens.presentation.feature.camera.ocr.contract.OcrProcessingUiEvent
import com.example.lagallens.presentation.feature.camera.ocr.contract.OcrProcessingUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OcrProcessingViewModel(
    private val ocrRepository: OcrRepository,
    private val uploadRepository: DocumentUploadRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(OcrProcessingUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<OcrProcessingUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    private var hasStarted = false

    fun onEvent(event: OcrProcessingUiEvent) {
        when (event) {
            is OcrProcessingUiEvent.ScreenOpened -> start(event)
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

    private fun start(event: OcrProcessingUiEvent.ScreenOpened) {
        if (hasStarted) return
        hasStarted = true
        viewModelScope.launch {
            val succeeded = try {
                uploadScannedPages(event) && recognizeText(event.documentId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                false
            }
            val state = _uiState.value
            _uiEffect.send(
                if (succeeded) OcrProcessingUiEffect.NavigateToOcrResult(state.documentName, state.documentDetails)
                else OcrProcessingUiEffect.NavigateToOcrFailure(state.documentName, state.documentDetails)
            )
        }
    }

    /** Files arrive already uploaded; scanned pages are uploaded here as the first processing step. */
    private suspend fun uploadScannedPages(event: OcrProcessingUiEvent.ScreenOpened): Boolean {
        if (event.pagePaths.isEmpty()) return true
        val request = DocumentUploadRequest(event.documentId, _uiState.value.documentName, SCANNED_PAGE_MIME_TYPE, event.pagePaths)
        return uploadRepository.upload(request).first { it !is ProcessingState.Running } == ProcessingState.Completed
    }

    private suspend fun recognizeText(documentId: String): Boolean {
        var succeeded = false
        ocrRepository.process(documentId).collect { state ->
            when (state) {
                is ProcessingState.Running -> state.stage?.let { stage -> _uiState.update { it.withActiveStep(stage) } }
                ProcessingState.Completed -> succeeded = true
                is ProcessingState.Failed -> succeeded = false
            }
        }
        return succeeded
    }

    class Factory(
        private val ocrRepository: OcrRepository,
        private val uploadRepository: DocumentUploadRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = OcrProcessingViewModel(ocrRepository, uploadRepository) as T
    }

    private companion object {
        const val SCANNED_PAGE_MIME_TYPE = "image/jpeg"
    }
}
