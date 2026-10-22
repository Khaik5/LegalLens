package com.example.lagallens.presentation.feature.ocr.result.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lagallens.domain.usecase.GetOcrResultUseCase
import com.example.lagallens.presentation.feature.ocr.result.contract.OcrResultUiEffect
import com.example.lagallens.presentation.feature.ocr.result.contract.OcrResultUiEvent
import com.example.lagallens.presentation.feature.ocr.result.contract.OcrResultUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OcrResultViewModel(
    private val getOcrResult: GetOcrResultUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(OcrResultUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<OcrResultUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: OcrResultUiEvent) {
        when (event) {
            is OcrResultUiEvent.ScreenOpened -> loadResult(event.documentName, event.documentDetails)
            OcrResultUiEvent.BackClicked -> emitEffect(OcrResultUiEffect.NavigateBack)
            OcrResultUiEvent.EditClicked -> emitEffect(OcrResultUiEffect.NavigateToEdit(_uiState.value.documentText))
            OcrResultUiEvent.ReviewClicked -> emitEffect(OcrResultUiEffect.NavigateToReview)
            is OcrResultUiEvent.DocumentUpdated -> _uiState.update { it.copy(documentText = event.documentText) }
            OcrResultUiEvent.ReviewConfirmed -> {
                _uiState.update { it.copy(isReviewConfirmed = true) }
                emitEffect(OcrResultUiEffect.NavigateToSaved(_uiState.value.documentName))
            }
        }
    }

    private fun loadResult(documentName: String, documentDetails: String) {
        viewModelScope.launch {
            val result = getOcrResult(documentName, documentDetails)
            _uiState.update {
                it.copy(
                    documentName = documentName,
                    documentDetails = documentDetails,
                    confidenceLabel = result.confidenceLabel,
                    confidencePercentage = result.confidencePercentage,
                    documentText = result.documentText
                )
            }
        }
    }

    private fun emitEffect(effect: OcrResultUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }

    class Factory(
        private val getOcrResult: GetOcrResultUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(OcrResultViewModel::class.java))
            return OcrResultViewModel(getOcrResult) as T
        }
    }
}
