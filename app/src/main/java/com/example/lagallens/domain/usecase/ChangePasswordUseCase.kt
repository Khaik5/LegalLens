package com.example.lagallens.domain.usecase

import com.example.lagallens.domain.repository.AuthRepository

class ChangePasswordUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(currentPassword: String, newPassword: String) =
        repository.changePassword(currentPassword, newPassword)
}
