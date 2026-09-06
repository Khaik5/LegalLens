package com.example.lagallens.presentation.feature.upload.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.upload.contract.UploadSourceEffect
import com.example.lagallens.presentation.feature.upload.contract.UploadSourceEvent
import com.example.lagallens.presentation.feature.upload.contract.UploadSourceUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UploadSourceViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UploadSourceUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<UploadSourceEffect>()
    val effects = _effects.asSharedFlow()

    fun onEvent(event: UploadSourceEvent) {
        when (event) {
            UploadSourceEvent.BackClicked -> emitEffect(UploadSourceEffect.NavigateBack)
            UploadSourceEvent.PdfClicked -> emitEffect(
                UploadSourceEffect.OpenDocumentPicker(arrayOf("application/pdf"))
            )
            UploadSourceEvent.DocxClicked -> emitEffect(
                UploadSourceEffect.OpenDocumentPicker(
                    arrayOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                )
            )
            UploadSourceEvent.GalleryClicked -> emitEffect(UploadSourceEffect.OpenImagePicker)
            UploadSourceEvent.CameraClicked -> emitEffect(UploadSourceEffect.ShowCameraUnavailable)
            UploadSourceEvent.FileSelected -> emitEffect(UploadSourceEffect.ShowFileSelected)
        }
    }

    private fun emitEffect(effect: UploadSourceEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }
}
