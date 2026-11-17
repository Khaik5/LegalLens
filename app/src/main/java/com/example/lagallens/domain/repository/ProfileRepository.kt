package com.example.lagallens.domain.repository

import com.example.lagallens.domain.model.UserProfile

interface ProfileRepository {
    suspend fun getProfile(): UserProfile
    suspend fun saveProfile(name: String, phone: String, photoUri: String?): UserProfile
    /** Removes the avatar and returns the profile as it is afterwards. */
    suspend fun removeAvatar(): UserProfile
    suspend fun clearSessionData(): Boolean = true

    /** False when the data source has no email verification, so the entry to change it stays hidden. */
    val canChangeEmail: Boolean get() = false

    /** Sends a verification code to [newEmail] after the current password is checked. */
    suspend fun requestEmailChange(newEmail: String, currentPassword: String): EmailChangeChallenge =
        throw UnsupportedOperationException("Changing the email needs the backend")

    /** Applies the change; the backend then ends every session, so the caller must sign in again. */
    suspend fun confirmEmailChange(challengeId: String, code: String): String =
        throw UnsupportedOperationException("Changing the email needs the backend")
}

class EmailChangeChallenge(val challengeId: String, val expiresInSeconds: Int?)

/** The current password is wrong, or the new address equals the current one. */
class EmailChangeRejectedException(cause: Exception) : Exception("The email change request was rejected", cause)

/** Another account already uses the new email address. */
class EmailAlreadyUsedException(cause: Exception) : Exception("The email address is already in use", cause)

/** The verification code is wrong, expired or already used. */
class EmailChangeCodeRejectedException(cause: Exception) : Exception("The verification code was rejected", cause)

/** The backend rejected the phone number; nothing was saved. */
class ProfilePhoneRejectedException(cause: Exception) : Exception("The phone number was rejected", cause)

/** The name and phone were saved, but the new avatar was not; [savedProfile] is what the backend now holds. */
class ProfileAvatarUpdateException(val savedProfile: UserProfile, cause: Exception) :
    Exception("Could not update the profile avatar", cause)
