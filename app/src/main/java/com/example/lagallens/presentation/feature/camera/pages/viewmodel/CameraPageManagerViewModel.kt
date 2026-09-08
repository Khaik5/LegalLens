package com.example.lagallens.presentation.feature.camera.pages.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.camera.core.ScanPage
import com.example.lagallens.presentation.feature.camera.pages.contract.CameraPageManagerEffect
import com.example.lagallens.presentation.feature.camera.pages.contract.CameraPageManagerEvent
import com.example.lagallens.presentation.feature.camera.pages.contract.CameraPageManagerUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CameraPageManagerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CameraPageManagerUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<CameraPageManagerEffect>()
    val effects = _effects.asSharedFlow()

    fun setPages(pages: List<ScanPage>) {
        _uiState.value = CameraPageManagerUiState(pages)
    }

    fun onEvent(event: CameraPageManagerEvent) {
        when (event) {
            CameraPageManagerEvent.BackClicked,
            CameraPageManagerEvent.AddPageClicked -> emitReturn()
            CameraPageManagerEvent.RetakeLastClicked -> {
                val last = _uiState.value.pages.lastOrNull() ?: return
                _uiState.update { it.copy(pages = it.pages.dropLast(1)) }
                emitReturn(listOf(last.path))
            }
            CameraPageManagerEvent.ClearAllClicked -> {
                val paths = _uiState.value.pages.map(ScanPage::path)
                _uiState.update { it.copy(pages = emptyList()) }
                emitReturn(paths)
            }
            is CameraPageManagerEvent.DeletePageClicked -> {
                _uiState.update { state -> state.copy(pages = state.pages.filterNot { it.path == event.path }) }
            }
            CameraPageManagerEvent.CompleteClicked -> {
                emitEffect(CameraPageManagerEffect.NavigateToOcrProcessing(_uiState.value.pages))
            }
        }
    }

    private fun emitReturn(deletedPaths: List<String> = emptyList()) {
        emitEffect(CameraPageManagerEffect.ReturnToCapture(_uiState.value.pages, deletedPaths))
    }

    private fun emitEffect(effect: CameraPageManagerEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }
}
