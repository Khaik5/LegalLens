package com.example.lagallens.presentation.feature.home.viewmodel

import androidx.lifecycle.ViewModel
import com.example.lagallens.presentation.feature.home.contract.HomeUiEffect
import com.example.lagallens.presentation.feature.home.contract.HomeUiEvent
import com.example.lagallens.presentation.feature.home.contract.HomeUiState
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<HomeUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            HomeUiEvent.LoginClicked -> sendEffect(HomeUiEffect.NavigateToLogin)
            HomeUiEvent.RegisterClicked -> sendEffect(HomeUiEffect.NavigateToRegister)
        }
    }

    private fun sendEffect(effect: HomeUiEffect) {
        viewModelScope.launch {
            _uiEffect.send(effect)
        }
    }
}
