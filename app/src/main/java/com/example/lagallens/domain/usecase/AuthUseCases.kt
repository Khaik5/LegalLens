package com.example.lagallens.domain.usecase

import com.example.lagallens.domain.model.auth.AuthResult
import com.example.lagallens.domain.model.auth.OtpPurpose
import com.example.lagallens.domain.repository.AuthRepository
import com.example.lagallens.domain.repository.ChatbotRepository
import com.example.lagallens.domain.repository.ProfileRepository

class RegisterAccountUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(fullName: String, email: String, password: String): AuthResult =
        repository.register(fullName, email, password)
}

class VerifyOtpUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, code: String, purpose: OtpPurpose): AuthResult =
        repository.verifyOtp(email, code, purpose)
}

class ResendOtpUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, purpose: OtpPurpose): AuthResult = repository.resendOtp(email, purpose)
}

class RequestPasswordResetUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String): AuthResult = repository.requestPasswordReset(email)
}

class ConfirmPasswordResetUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, newPassword: String): AuthResult =
        repository.confirmPasswordReset(email, newPassword)
}

class LogoutUseCase(
    private val authRepository: AuthRepository,
    private val chatbotRepository: ChatbotRepository,
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(): Boolean {
        chatbotRepository.clearSessionData()
        val profileCleared = profileRepository.clearSessionData()
        return authRepository.logout() && profileCleared
    }
}
