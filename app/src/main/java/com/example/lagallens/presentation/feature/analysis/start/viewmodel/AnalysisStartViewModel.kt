package com.example.lagallens.presentation.feature.analysis.start.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.presentation.feature.analysis.start.contract.AnalysisStartUiEffect
import com.example.lagallens.presentation.feature.analysis.start.contract.AnalysisStartUiEvent
import com.example.lagallens.presentation.feature.analysis.start.contract.AnalysisStartUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AnalysisStartViewModel(private val isAnalysisAvailable: Boolean = true) : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisStartUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<AnalysisStartUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun onEvent(event: AnalysisStartUiEvent) {
        when (event) {
            is AnalysisStartUiEvent.ScreenOpened -> _uiState.update {
                it.copy(
                    documentName = event.documentName,
                    documentMetadata = event.documentMetadata
                )
            }

            AnalysisStartUiEvent.BackClicked -> emitEffect(AnalysisStartUiEffect.NavigateBack)
            AnalysisStartUiEvent.StartAnalysisClicked -> {
                if (!isAnalysisAvailable) {
                    emitEffect(AnalysisStartUiEffect.ShowMessage(R.string.analysis_ai_unavailable))
                    return
                }
                val state = _uiState.value
                emitEffect(
                    AnalysisStartUiEffect.NavigateToAnalysisProcessing(
                        documentName = state.documentName,
                        documentMetadata = state.documentMetadata
                    )
                )
            }
        }
    }

    private fun emitEffect(effect: AnalysisStartUiEffect) {
        viewModelScope.launch { _uiEffect.send(effect) }
    }

    class Factory(private val isAnalysisAvailable: Boolean) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AnalysisStartViewModel::class.java))
            return AnalysisStartViewModel(isAnalysisAvailable) as T
        }
    }
}
