package com.example.lagallens.di

import com.example.lagallens.data.datasource.local.datastore.AuthLocalDataSource
import com.example.lagallens.data.repository.LocalAuthRepository
import com.example.lagallens.domain.repository.AuthRepository

object RepositoryModule {
    fun provideAuthRepository(): AuthRepository {
        return LocalAuthRepository(AuthLocalDataSource())
    }
}
