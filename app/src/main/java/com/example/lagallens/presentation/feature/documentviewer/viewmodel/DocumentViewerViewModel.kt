package com.example.lagallens.presentation.feature.documentviewer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lagallens.core.network.DocumentFiles
import com.example.lagallens.domain.usecase.GetDocumentFileUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import com.example.lagallens.presentation.feature.documentviewer.contract.DocumentViewerStatus
import com.example.lagallens.presentation.feature.documentviewer.contract.DocumentViewerUiEffect
import com.example.lagallens.presentation.feature.documentviewer.contract.DocumentViewerUiEvent
import com.example.lagallens.presentation.feature.documentviewer.contract.DocumentViewerUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DocumentViewerViewModel(private val getDocumentFile: GetDocumentFileUseCase) : ViewModel() {
    private var remoteRequest: DocumentViewerUiEvent.OpenRemote? = null
    private var previewJob: Job? = null
    private val state = MutableStateFlow(DocumentViewerUiState())
    val uiState = state.asStateFlow()
    private val effects = Channel<DocumentViewerUiEffect>(Channel.BUFFERED)
    val uiEffect = effects.receiveAsFlow()

    private fun loadPreview(request: DocumentViewerUiEvent.OpenRemote) {
        previewJob?.cancel()
        releasePreview()
        previewJob = viewModelScope.launch {
            state.update { it.copy(status = DocumentViewerStatus.LOADING, remoteDocument = null, previewUri = null) }
            try {
                val document = getDocumentFile.findForContract(request.contractId, request.sessionKey)
                state.update { it.copy(remoteDocument = document, title = document?.displayName ?: it.title) }
                // Word files are previewed as the PDF the backend converts them to.
                if (document == null || !DocumentFiles.supportsOcr(document.mimeType)) {
                    state.update { it.copy(status = DocumentViewerStatus.ERROR) }
                    return@launch
                }
                val uri = getDocumentFile(document.id, request.sessionKey)
                state.update { it.copy(previewUri = uri) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                state.update { it.copy(status = DocumentViewerStatus.ERROR) }
            }
        }
    }

    private fun download() {
        val session = remoteRequest?.sessionKey ?: return
        val document = state.value.remoteDocument ?: return
        if (state.value.isDownloading) return
        state.update { it.copy(isDownloading = true) }
        viewModelScope.launch {
            try {
                val uri = getDocumentFile(document.id, session, forDownload = true)
                effects.send(DocumentViewerUiEffect.SaveDownload(uri, document.displayName, document.mimeType))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                effects.send(DocumentViewerUiEffect.DownloadFailed)
            } finally {
                state.update { it.copy(isDownloading = false) }
            }
        }
    }

    /** Each preview is a fresh cache file, so drop the previous one once it is no longer shown. */
    private fun releasePreview() {
        state.value.previewUri?.let(getDocumentFile::release)
    }

    override fun onCleared() {
        previewJob?.cancel()
        releasePreview()
        super.onCleared()
    }

    class Factory(private val getDocumentFile: GetDocumentFileUseCase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = DocumentViewerViewModel(getDocumentFile) as T
    }

    fun onEvent(event: DocumentViewerUiEvent) {
        when (event) {
            is DocumentViewerUiEvent.Opened -> if (!state.value.isInitialized) {
                state.update { it.copy(title = event.title, metadata = event.metadata, pageCount = event.pageCount.coerceAtLeast(1), status = DocumentViewerStatus.ERROR, isInitialized = true) }
            }
            is DocumentViewerUiEvent.PdfLoaded -> state.update {
                it.copy(title = event.title, metadata = event.metadata, pageCount = event.pageCount.coerceAtLeast(1), currentPage = 1, zoomPercent = 100, status = DocumentViewerStatus.READY, isInitialized = true)
            }
            DocumentViewerUiEvent.PdfLoading -> state.update { it.copy(status = DocumentViewerStatus.LOADING) }
            DocumentViewerUiEvent.PdfLoadFailed -> state.update { it.copy(status = DocumentViewerStatus.ERROR) }
            is DocumentViewerUiEvent.OpenRemote -> if (remoteRequest != event) {
                remoteRequest = event
                loadPreview(event)
            }
            DocumentViewerUiEvent.RetryClicked -> remoteRequest?.let(::loadPreview) ?: Unit
            DocumentViewerUiEvent.DownloadClicked -> download()
            DocumentViewerUiEvent.PreviousClicked -> state.update { it.copy(currentPage = (it.currentPage - 1).coerceAtLeast(1)) }
            DocumentViewerUiEvent.NextClicked -> state.update { it.copy(currentPage = (it.currentPage + 1).coerceAtMost(it.pageCount)) }
            DocumentViewerUiEvent.ZoomOutClicked -> state.update { it.copy(zoomPercent = (it.zoomPercent - 25).coerceAtLeast(75)) }
            DocumentViewerUiEvent.ZoomInClicked -> state.update { it.copy(zoomPercent = (it.zoomPercent + 25).coerceAtMost(150)) }
            DocumentViewerUiEvent.BackClicked -> viewModelScope.launch { effects.send(DocumentViewerUiEffect.NavigateBack) }
        }
    }

}
