package com.example.lagallens.presentation.feature.home

object HomeContract {
    data class State(
        val isLoading: Boolean = false
    )

    sealed interface Event {
        data object LoginClicked : Event
        data object RegisterClicked : Event
    }

    sealed interface Effect {
        data object NavigateToRegister : Effect
        data object ShowLoginComingSoon : Effect
    }
}
