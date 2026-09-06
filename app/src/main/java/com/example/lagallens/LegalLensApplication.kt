package com.example.lagallens

import android.app.Application
import com.example.lagallens.di.RepositoryModule
import com.example.lagallens.di.UseCaseModule
import com.example.lagallens.domain.repository.AuthRepository
import com.example.lagallens.domain.usecase.AuthenticateUserUseCase

class LegalLensApplication : Application() {
    private val authRepository: AuthRepository by lazy { RepositoryModule.provideAuthRepository() }
    val authenticateUserUseCase: AuthenticateUserUseCase by lazy {
        UseCaseModule.provideAuthenticateUserUseCase(authRepository)
    }
}
