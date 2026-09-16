package com.example.lagallens.data.repository

import com.example.lagallens.data.datasource.local.datastore.AuthLocalDataSource
import com.example.lagallens.domain.repository.AuthRepository

class LocalAuthRepository(
    private val authLocalDataSource: AuthLocalDataSource
) : AuthRepository {

    override fun authenticate(username: String, password: String): Boolean {
        val localAccount = authLocalDataSource.getAccount()
        return username == localAccount.username && password == localAccount.password
    }
}
