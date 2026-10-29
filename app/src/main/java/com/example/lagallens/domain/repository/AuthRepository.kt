package com.example.lagallens.domain.repository

interface AuthRepository {
    fun authenticate(username: String, password: String): Boolean

    fun currentUserEmail(): String? = null

    fun currentUserId(): String? = null

    fun logout(): Boolean = true

    fun changePassword(currentPassword: String, newPassword: String): Boolean = false
}
