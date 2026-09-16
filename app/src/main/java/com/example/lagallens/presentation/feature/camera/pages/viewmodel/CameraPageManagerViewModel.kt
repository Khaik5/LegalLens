package com.example.lagallens.presentation.feature.camera.pages.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.domain.model.camera.ScanPage
import com.example.lagallens.presentation.feature.camera.pages.contract.CameraPageManagerUiEffect
import com.example.lagallens.presentation.feature.camera.pages.contract.CameraPageManagerUiEvent
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

    private val _uiEffect = MutableSharedFlow<CameraPageManagerUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun setPages(pages: List<ScanPage>) {
        _uiState.value = CameraPageManagerUiState(pages)
    }

    fun onEvent(event: CameraPageManagerUiEvent) {
        when (event) {
            CameraPageManagerUiEvent.BackClicked,
            CameraPageManagerUiEvent.AddPageClicked -> emitReturn()
            CameraPageManagerUiEvent.RetakeLastClicked -> {
                val last = _uiState.value.pages.lastOrNull() ?: return
                _uiState.update { it.copy(pages = it.pages.dropLast(1)) }
                emitReturn(listOf(last.path))
            }
            CameraPageManagerUiEvent.ClearAllClicked -> {
                val paths = _uiState.value.pages.map(ScanPage::path)
                _uiState.update { it.copy(pages = emptyList()) }
                emitReturn(paths)
            }
            is CameraPageManagerUiEvent.DeletePageClicked -> {
                _uiState.update { state -> state.copy(pages = state.pages.filterNot { it.path == event.path }) }
            }
            CameraPageManagerUiEvent.CompleteClicked -> {
                emitEffect(CameraPageManagerUiEffect.NavigateToOcrProcessing(_uiState.value.pages))
            }
        }
    }

    private fun emitReturn(deletedPaths: List<String> = emptyList()) {
        emitEffect(CameraPageManagerUiEffect.ReturnToCapture(_uiState.value.pages, deletedPaths))
    }

    private fun emitEffect(effect: CameraPageManagerUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }
}
