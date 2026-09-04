package com.example.lagallens.presentation.feature.register

object RegisterContract {
    data class State(
        val isLoading: Boolean = false
    )

    sealed interface Event {
        data object BackClicked : Event
        data object SubmitClicked : Event
        data object LoginClicked : Event
    }

    sealed interface Effect {
        data object CloseScreen : Effect
        data object ShowRegisterComingSoon : Effect
    }
}
