package com.example.lagallens.domain.usecase

import com.example.lagallens.domain.repository.AuthRepository

class AuthenticateUserUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(username: String, password: String): Boolean {
        return authRepository.authenticate(username, password)
    }
}
