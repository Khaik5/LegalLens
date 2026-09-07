package com.example.lagallens.domain.repository

interface AuthRepository {
    fun authenticate(username: String, password: String): Boolean
}
