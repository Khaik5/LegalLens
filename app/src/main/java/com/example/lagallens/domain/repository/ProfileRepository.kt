package com.example.lagallens.domain.repository

import com.example.lagallens.domain.model.UserProfile

interface ProfileRepository {
    suspend fun getProfile(): UserProfile
    suspend fun saveProfile(name: String, phone: String, photoUri: String?): UserProfile
    suspend fun clearSessionData(): Boolean = true
}

/** The backend rejected the phone number; nothing was saved. */
class ProfilePhoneRejectedException(cause: Exception) : Exception("The phone number was rejected", cause)

/** The name and phone were saved, but the new avatar was not; [savedProfile] is what the backend now holds. */
class ProfileAvatarUpdateException(val savedProfile: UserProfile, cause: Exception) :
    Exception("Could not update the profile avatar", cause)
