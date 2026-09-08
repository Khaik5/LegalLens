package com.example.lagallens.presentation.feature.camera.ocr.viewmodel

import androidx.lifecycle.ViewModel
import com.example.lagallens.presentation.feature.camera.ocr.contract.OcrProcessingUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class OcrProcessingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(OcrProcessingUiState())
    val uiState = _uiState.asStateFlow()

    fun setPageCount(pageCount: Int) {
        _uiState.value = OcrProcessingUiState(pageCount)
    }
}
