package com.example.lagallens.presentation.feature.documentviewer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.documentviewer.contract.DocumentViewerStatus
import com.example.lagallens.presentation.feature.documentviewer.contract.DocumentViewerUiEffect
import com.example.lagallens.presentation.feature.documentviewer.contract.DocumentViewerUiEvent
import com.example.lagallens.presentation.feature.documentviewer.contract.DocumentViewerUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DocumentViewerViewModel : ViewModel() {
    private val state = MutableStateFlow(DocumentViewerUiState())
    val uiState = state.asStateFlow()
    private val effects = MutableSharedFlow<DocumentViewerUiEffect>()
    val uiEffect = effects.asSharedFlow()

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
            DocumentViewerUiEvent.RetryClicked -> Unit
            DocumentViewerUiEvent.PreviousClicked -> state.update { it.copy(currentPage = (it.currentPage - 1).coerceAtLeast(1)) }
            DocumentViewerUiEvent.NextClicked -> state.update { it.copy(currentPage = (it.currentPage + 1).coerceAtMost(it.pageCount)) }
            DocumentViewerUiEvent.ZoomOutClicked -> state.update { it.copy(zoomPercent = (it.zoomPercent - 25).coerceAtLeast(75)) }
            DocumentViewerUiEvent.ZoomInClicked -> state.update { it.copy(zoomPercent = (it.zoomPercent + 25).coerceAtMost(150)) }
            DocumentViewerUiEvent.BackClicked -> viewModelScope.launch { effects.emit(DocumentViewerUiEffect.NavigateBack) }
        }
    }

}
