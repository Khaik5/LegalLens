package com.example.lagallens.presentation.feature.upload.progress.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.upload.progress.contract.UploadProgressEffect
import com.example.lagallens.presentation.feature.upload.progress.contract.UploadProgressEvent
import com.example.lagallens.presentation.feature.upload.progress.contract.UploadProgressUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class UploadProgressViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UploadProgressUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<UploadProgressEffect>()
    val effects = _effects.asSharedFlow()
    private var localPreviewJob: Job? = null

    fun onEvent(event: UploadProgressEvent) {
        when (event) {
            is UploadProgressEvent.ScreenOpened -> {
                _uiState.update {
                    it.copy(fileName = event.fileName, fileDetails = event.fileDetails)
                }
                startLocalOcrPreview(event.fileName, event.fileDetails)
            }

            UploadProgressEvent.CancelClicked -> {
                localPreviewJob?.cancel()
                emitEffect(UploadProgressEffect.NavigateBack)
            }
        }
    }

    private fun startLocalOcrPreview(fileName: String, fileDetails: String) {
        localPreviewJob?.cancel()
        localPreviewJob = viewModelScope.launch {
            delay(700)
            _uiState.update { it.copy(progress = 100) }
            delay(300)
            _effects.emit(UploadProgressEffect.NavigateToOcrProcessing(fileName, fileDetails))
        }
    }

    private fun emitEffect(effect: UploadProgressEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }
}
