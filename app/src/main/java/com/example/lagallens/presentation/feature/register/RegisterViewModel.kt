package com.example.lagallens.presentation.feature.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {
    private val _state = MutableStateFlow(RegisterContract.State())
    val state: StateFlow<RegisterContract.State> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<RegisterContract.Effect>()
    val effect: SharedFlow<RegisterContract.Effect> = _effect.asSharedFlow()

    fun onEvent(event: RegisterContract.Event) {
        when (event) {
            RegisterContract.Event.BackClicked -> sendEffect(RegisterContract.Effect.CloseScreen)
            RegisterContract.Event.LoginClicked -> sendEffect(RegisterContract.Effect.CloseScreen)
            RegisterContract.Event.SubmitClicked -> sendEffect(RegisterContract.Effect.ShowRegisterComingSoon)
        }
    }

    private fun sendEffect(effect: RegisterContract.Effect) {
        viewModelScope.launch {
            _effect.emit(effect)
        }
    }
}
