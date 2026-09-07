package com.example.lagallens.presentation.feature.upload.error.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.upload.error.contract.UploadErrorEffect
import com.example.lagallens.presentation.feature.upload.error.contract.UploadErrorEvent
import com.example.lagallens.presentation.feature.upload.error.contract.UploadErrorUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UploadErrorViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UploadErrorUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<UploadErrorEffect>()
    val effects = _effects.asSharedFlow()

    fun onEvent(event: UploadErrorEvent) {
        when (event) {
            is UploadErrorEvent.ScreenOpened -> _uiState.update {
                it.copy(errorCode = event.errorCode, errorMessage = event.errorMessage)
            }

            UploadErrorEvent.RetryClicked,
            UploadErrorEvent.BackClicked -> emitEffect(UploadErrorEffect.OpenUploadSource)

            UploadErrorEvent.BackToListClicked -> emitEffect(UploadErrorEffect.ReturnToCaller)
        }
    }

    private fun emitEffect(effect: UploadErrorEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }
}
