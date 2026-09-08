package com.example.lagallens.presentation.feature.camera.permission.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.camera.permission.contract.CameraPermissionEffect
import com.example.lagallens.presentation.feature.camera.permission.contract.CameraPermissionEvent
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

    private val _effects = MutableSharedFlow<CameraPermissionEffect>()
    val effects = _effects.asSharedFlow()

    fun onEvent(event: CameraPermissionEvent) {
        when (event) {
            CameraPermissionEvent.AllowClicked -> {
                _uiState.update { it.copy(isRequestingPermission = true) }
                emitEffect(CameraPermissionEffect.RequestCameraPermission)
            }
            CameraPermissionEvent.LaterClicked -> emitEffect(CameraPermissionEffect.NavigateBack)
            is CameraPermissionEvent.PermissionResult -> {
                _uiState.update { it.copy(isRequestingPermission = false) }
                emitEffect(
                    if (event.granted) {
                        CameraPermissionEffect.NavigateToCameraCapture
                    } else {
                        CameraPermissionEffect.ShowPermissionDenied
                    }
                )
            }
        }
    }

    private fun emitEffect(effect: CameraPermissionEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }
}
