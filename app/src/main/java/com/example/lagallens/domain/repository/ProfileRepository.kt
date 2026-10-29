package com.example.lagallens.domain.repository

import com.example.lagallens.domain.model.UserProfile

interface ProfileRepository {
    fun getProfile(): UserProfile
    fun saveProfile(name: String, phone: String, photoUri: String?): UserProfile
    fun clearSessionData(): Boolean = true
}
