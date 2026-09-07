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
import kotlinx.coroutines.launch

class UploadProgressViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UploadProgressUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<UploadProgressEffect>()
    val effects = _effects.asSharedFlow()

    fun onEvent(event: UploadProgressEvent) {
        when (event) {
            is UploadProgressEvent.ScreenOpened -> _uiState.update {
                it.copy(fileName = event.fileName, fileDetails = event.fileDetails)
            }

            UploadProgressEvent.CancelClicked -> emitEffect(UploadProgressEffect.NavigateBack)
        }
    }

    private fun emitEffect(effect: UploadProgressEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }
}
