package com.example.lagallens.domain.usecase

import com.example.lagallens.domain.repository.AuthRepository

class RestoreSessionUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke() = repository.restoreSession()
}
