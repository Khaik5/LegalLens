package com.example.lagallens.presentation.feature.login.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.lagallens.domain.usecase.AuthenticateUserUseCase

class LoginViewModelFactory(
    private val authenticateUser: AuthenticateUserUseCase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(LoginViewModel::class.java))
        return LoginViewModel(authenticateUser) as T
    }
}
