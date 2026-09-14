package com.example.lagallens.presentation.feature.upload.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.upload.contract.UploadSourceUiEffect
import com.example.lagallens.presentation.feature.upload.contract.UploadSourceUiEvent
import com.example.lagallens.presentation.feature.upload.contract.UploadSourceUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UploadSourceViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UploadSourceUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<UploadSourceUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: UploadSourceUiEvent) {
        when (event) {
            UploadSourceUiEvent.BackClicked -> emitEffect(UploadSourceUiEffect.NavigateBack)
            UploadSourceUiEvent.PdfClicked -> emitEffect(
                UploadSourceUiEffect.OpenDocumentPicker(arrayOf("application/pdf"))
            )
            UploadSourceUiEvent.DocxClicked -> emitEffect(
                UploadSourceUiEffect.OpenDocumentPicker(
                    arrayOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                )
            )
            UploadSourceUiEvent.GalleryClicked -> emitEffect(UploadSourceUiEffect.OpenImagePicker)
            UploadSourceUiEvent.CameraClicked -> emitEffect(UploadSourceUiEffect.NavigateToCameraPermission)
            UploadSourceUiEvent.FileSelected -> emitEffect(UploadSourceUiEffect.ShowFileSelected)
        }
    }

    private fun emitEffect(effect: UploadSourceUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }
}
