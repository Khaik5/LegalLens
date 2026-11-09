package com.example.lagallens.domain.repository

import com.example.lagallens.domain.model.UserProfile

interface ProfileRepository {
    suspend fun getProfile(): UserProfile
    suspend fun saveProfile(name: String, phone: String, photoUri: String?): UserProfile
    suspend fun clearSessionData(): Boolean = true
}
