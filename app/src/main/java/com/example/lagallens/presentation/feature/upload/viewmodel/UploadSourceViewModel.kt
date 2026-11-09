package com.example.lagallens.presentation.feature.upload.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.upload.contract.UploadSourceUiEffect
import com.example.lagallens.presentation.feature.upload.contract.UploadSourceUiEvent
import com.example.lagallens.presentation.feature.upload.contract.UploadSourceUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UploadSourceViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UploadSourceUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<UploadSourceUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

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
        viewModelScope.launch { _uiEffect.send(effect) }
    }
}
