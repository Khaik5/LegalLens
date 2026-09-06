package com.example.lagallens.di

import com.example.lagallens.domain.repository.AuthRepository
import com.example.lagallens.domain.usecase.AuthenticateUserUseCase

object UseCaseModule {
    fun provideAuthenticateUserUseCase(repository: AuthRepository): AuthenticateUserUseCase {
        return AuthenticateUserUseCase(repository)
    }
}
