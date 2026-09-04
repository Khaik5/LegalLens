package com.example.lagallens.presentation.feature.forgotpassword

object ForgotPasswordContract {
    data class State(
        val email: String = "",
        val isLoading: Boolean = false
    )

    sealed interface Event {
        data object BackClicked : Event
        data class EmailChanged(val value: String) : Event
        data object SubmitClicked : Event
        data object LoginClicked : Event
    }

    sealed interface Effect {
        data object CloseScreen : Effect
        data object ShowResetLinkComingSoon : Effect
    }
}
