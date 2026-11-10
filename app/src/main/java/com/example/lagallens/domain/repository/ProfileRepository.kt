package com.example.lagallens.domain.repository

import com.example.lagallens.domain.model.UserProfile

interface ProfileRepository {
    suspend fun getProfile(): UserProfile
    suspend fun saveProfile(name: String, phone: String, photoUri: String?): UserProfile
    suspend fun clearSessionData(): Boolean = true
}

class ProfileNameUpdateException(val savedProfile: UserProfile, cause: Exception) :
    Exception("Could not update the profile name", cause)
