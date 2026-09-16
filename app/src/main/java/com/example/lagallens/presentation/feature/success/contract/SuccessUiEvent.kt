package com.example.lagallens.presentation.feature.success.contract

sealed interface SuccessUiEvent {
    data object StartUsingClicked : SuccessUiEvent
}
