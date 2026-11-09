package com.example.lagallens.presentation.feature.upload.progress.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lagallens.domain.model.processing.DocumentUploadRequest
import com.example.lagallens.domain.model.processing.ProcessingState
import com.example.lagallens.domain.repository.DocumentUploadRepository
import com.example.lagallens.presentation.feature.upload.progress.contract.UploadProgressEffect
import com.example.lagallens.presentation.feature.upload.progress.contract.UploadProgressEvent
import com.example.lagallens.presentation.feature.upload.progress.contract.UploadProgressUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UploadProgressViewModel(private val uploadRepository: DocumentUploadRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(UploadProgressUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<UploadProgressEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()
    private var uploadJob: Job? = null
    /** The document whose upload already ended and navigated; a recreated screen must not upload it again. */
    private var finishedDocumentId: String? = null

    fun onEvent(event: UploadProgressEvent) {
        when (event) {
            is UploadProgressEvent.ScreenOpened -> {
                if (event.documentId == finishedDocumentId) return
                val current = _uiState.value
                if (uploadJob?.isActive == true && current.fileName == event.fileName &&
                    current.fileDetails == event.fileDetails && current.documentUri == event.documentUri && current.documentId == event.documentId) return
                _uiState.update {
                    it.copy(fileName = event.fileName, fileDetails = event.fileDetails, documentUri = event.documentUri, documentId = event.documentId)
                }
                startUpload(event)
            }

            UploadProgressEvent.CancelClicked -> {
                uploadJob?.cancel()
                _effects.trySend(UploadProgressEffect.NavigateBack)
            }
        }
    }

    private fun startUpload(event: UploadProgressEvent.ScreenOpened) {
        uploadJob?.cancel()
        uploadJob = viewModelScope.launch {
            val request = DocumentUploadRequest(event.documentId, event.fileName, event.mimeType, listOfNotNull(event.documentUri))
            var completed = false
            try {
                uploadRepository.upload(request).collect { state ->
                    when (state) {
                        is ProcessingState.Running -> state.progressPercent?.let { percent -> _uiState.update { it.copy(progress = percent) } }
                        ProcessingState.Completed -> completed = true
                        is ProcessingState.Failed -> completed = false
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                completed = false
            }
            finishedDocumentId = event.documentId
            _effects.send(
                if (completed) UploadProgressEffect.NavigateToOcrProcessing(event.fileName, event.fileDetails, event.documentUri, event.documentId, event.mimeType)
                else UploadProgressEffect.NavigateToUploadError
            )
        }
    }

    class Factory(private val uploadRepository: DocumentUploadRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = UploadProgressViewModel(uploadRepository) as T
    }
}
