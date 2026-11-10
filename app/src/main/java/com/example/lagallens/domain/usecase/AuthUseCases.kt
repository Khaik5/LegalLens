package com.example.lagallens.domain.usecase

import com.example.lagallens.domain.model.auth.OtpPurpose
import com.example.lagallens.domain.repository.AuthRepository
import com.example.lagallens.domain.repository.ChatbotRepository
import com.example.lagallens.domain.repository.ProfileRepository

class RegisterAccountUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(fullName: String, email: String, password: String) =
        repository.register(fullName, email, password)
}

class VerifyOtpUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(challengeId: String, code: String, purpose: OtpPurpose) =
        repository.verifyOtp(challengeId, code, purpose)
}

class ResendOtpUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, purpose: OtpPurpose) = repository.resendOtp(email, purpose)
}

class RequestPasswordResetUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String) = repository.requestPasswordReset(email)
}

class ConfirmPasswordResetUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(challengeId: String, verificationToken: String, newPassword: String) =
        repository.confirmPasswordReset(challengeId, verificationToken, newPassword)
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
