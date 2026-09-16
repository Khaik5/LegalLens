package com.example.lagallens.presentation.feature.camera.permission.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.camera.permission.contract.CameraPermissionUiEffect
import com.example.lagallens.presentation.feature.camera.permission.contract.CameraPermissionUiEvent
import com.example.lagallens.presentation.feature.camera.permission.contract.CameraPermissionUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CameraPermissionViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CameraPermissionUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<CameraPermissionUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: CameraPermissionUiEvent) {
        when (event) {
            CameraPermissionUiEvent.AllowClicked -> {
                _uiState.update { it.copy(isRequestingPermission = true) }
                emitEffect(CameraPermissionUiEffect.RequestCameraPermission)
            }
            CameraPermissionUiEvent.LaterClicked -> emitEffect(CameraPermissionUiEffect.NavigateBack)
            is CameraPermissionUiEvent.PermissionResult -> {
                _uiState.update { it.copy(isRequestingPermission = false) }
                emitEffect(
                    if (event.granted) {
                        CameraPermissionUiEffect.NavigateToCameraCapture
                    } else {
                        CameraPermissionUiEffect.ShowPermissionDenied
                    }
                )
            }
        }
    }

    private fun emitEffect(effect: CameraPermissionUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }
}
