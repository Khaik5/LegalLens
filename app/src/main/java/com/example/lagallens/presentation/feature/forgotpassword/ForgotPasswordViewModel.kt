package com.example.lagallens.presentation.feature.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ForgotPasswordViewModel : ViewModel() {
    private val _state = MutableStateFlow(ForgotPasswordContract.State())
    val state: StateFlow<ForgotPasswordContract.State> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<ForgotPasswordContract.Effect>()
    val effect: SharedFlow<ForgotPasswordContract.Effect> = _effect.asSharedFlow()

    fun onEvent(event: ForgotPasswordContract.Event) {
        when (event) {
            ForgotPasswordContract.Event.BackClicked -> sendEffect(ForgotPasswordContract.Effect.CloseScreen)
            is ForgotPasswordContract.Event.EmailChanged -> _state.update { it.copy(email = event.value) }
            ForgotPasswordContract.Event.SubmitClicked -> sendEffect(ForgotPasswordContract.Effect.ShowResetLinkComingSoon)
            ForgotPasswordContract.Event.LoginClicked -> sendEffect(ForgotPasswordContract.Effect.CloseScreen)
        }
    }

    private fun sendEffect(effect: ForgotPasswordContract.Effect) {
        viewModelScope.launch {
            _effect.emit(effect)
        }
    }
}
