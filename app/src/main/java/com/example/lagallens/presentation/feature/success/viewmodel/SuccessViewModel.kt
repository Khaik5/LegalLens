package com.example.lagallens.presentation.feature.success.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.lagallens.presentation.feature.success.contract.SuccessUiEffect
import com.example.lagallens.presentation.feature.success.contract.SuccessUiEvent
import com.example.lagallens.presentation.feature.success.contract.SuccessUiState

class SuccessViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SuccessUiState())
    val uiState: StateFlow<SuccessUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<SuccessUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun onEvent(event: SuccessUiEvent) {
        when (event) {
            SuccessUiEvent.StartUsingClicked -> sendEffect(SuccessUiEffect.NavigateToHome)
        }
    }

    private fun sendEffect(effect: SuccessUiEffect) {
        viewModelScope.launch {
            _uiEffect.send(effect)
        }
    }
}
