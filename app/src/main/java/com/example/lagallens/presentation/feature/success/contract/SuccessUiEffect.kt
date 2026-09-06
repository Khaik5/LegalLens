package com.example.lagallens.presentation.feature.success.contract

sealed interface SuccessUiEffect {
    data object NavigateToHome : SuccessUiEffect
}
