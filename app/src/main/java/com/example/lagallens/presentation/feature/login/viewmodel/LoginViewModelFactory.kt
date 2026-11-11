package com.example.lagallens.presentation.feature.login.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.lagallens.domain.usecase.AuthenticateUserUseCase
import com.example.lagallens.domain.usecase.ResendOtpUseCase

class LoginViewModelFactory(
    private val authenticateUser: AuthenticateUserUseCase,
    private val resendOtp: ResendOtpUseCase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(LoginViewModel::class.java))
        return LoginViewModel(authenticateUser, resendOtp) as T
    }
}
