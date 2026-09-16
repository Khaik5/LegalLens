package com.example.lagallens.presentation.feature.home.contract

sealed interface HomeUiEvent {
    data object LoginClicked : HomeUiEvent
    data object RegisterClicked : HomeUiEvent
}
