package com.example.lagallens.presentation.feature.success.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.lagallens.presentation.feature.success.contract.SuccessUiEffect
import com.example.lagallens.presentation.feature.success.contract.SuccessUiEvent
import com.example.lagallens.presentation.feature.success.contract.SuccessUiState

class SuccessViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SuccessUiState())
    val uiState: StateFlow<SuccessUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<SuccessUiEffect>()
    val uiEffect: SharedFlow<SuccessUiEffect> = _uiEffect.asSharedFlow()

    fun onEvent(event: SuccessUiEvent) {
        when (event) {
            SuccessUiEvent.StartUsingClicked -> sendEffect(SuccessUiEffect.NavigateToHome)
        }
    }

    private fun sendEffect(effect: SuccessUiEffect) {
        viewModelScope.launch {
            _uiEffect.emit(effect)
        }
    }
}
