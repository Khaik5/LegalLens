package com.example.lagallens.data.repository

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.core.network.HttpFailureKind
import com.example.lagallens.core.network.httpFailureKind
import com.example.lagallens.data.datasource.local.ProfileAvatarStore
import com.example.lagallens.data.datasource.local.datastore.AuthSessionStore
import com.example.lagallens.data.datasource.local.datastore.ProfileLocalEdits
import com.example.lagallens.data.datasource.local.datastore.currentSessionScope
import com.example.lagallens.data.datasource.local.datastore.requireUserId
import com.example.lagallens.data.datasource.remote.UserRemoteDataSource
import com.example.lagallens.data.mapper.toProfile
import com.example.lagallens.data.model.request.UpdateMyProfileRequest
import com.example.lagallens.data.model.response.UserResponse
import com.example.lagallens.domain.repository.ProfileAvatarUpdateException
import com.example.lagallens.domain.repository.ProfilePhoneRejectedException
import com.example.lagallens.domain.repository.ProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.HttpException

/** Name, phone number and avatar all live on the backend; the avatar image is cached for the session only. */
class RemoteProfileRepository internal constructor(
    private val tokens: AuthSessionStore,
    private val remote: UserRemoteDataSource,
    private val avatars: ProfileAvatarStore,
    private val deviceEdits: ProfileLocalEdits
) : ProfileRepository {
    private val edits = Mutex()
    private var lastProfile: LoadedProfile? = null
    private var clearedSession: String? = null

    private class LoadedProfile(val sessionKey: String, val user: UserResponse, val photoUri: String?)

    override suspend fun getProfile() = withContext(Dispatchers.IO) {
        val scope = checkNotNull(tokens.currentSessionScope()) { "No active session" }
        val userId = tokens.requireUserId(scope)
        edits.withLock {
            val fetched = remote.me(scope)
            checkOwner(scope, userId, fetched)
            val user = moveDeviceEdits(scope, userId, fetched)
            val photoUri = avatarOf(scope, userId, user)
            lastProfile = LoadedProfile(scope.key, user, photoUri)
            user.toProfile(photoUri)
        }
    }

    override suspend fun saveProfile(name: String, phone: String, photoUri: String?) = withContext(Dispatchers.IO) {
        val scope = checkNotNull(tokens.currentSessionScope()) { "No active session" }
        val userId = tokens.requireUserId(scope)
        edits.withLock {
            check(tokens.requireUserId(scope) == userId && clearedSession != scope.key)
            val loaded = lastProfile?.takeIf { it.sessionKey == scope.key }
            val current = loaded?.user ?: remote.me(scope)
            checkOwner(scope, userId, current)
            val trimmedName = name.trim()
            val trimmedPhone = phone.trim()
            val user = if (current.name == trimmedName && current.phone.orEmpty() == trimmedPhone) {
                current
            } else {
                updateDetails(scope, trimmedName, trimmedPhone, isPhoneChanged = current.phone.orEmpty() != trimmedPhone)
            }
            checkOwner(scope, userId, user)
            val currentPhoto = if (loaded != null) loaded.photoUri else avatarOf(scope, userId, user)
            lastProfile = LoadedProfile(scope.key, user, currentPhoto)
            if (photoUri == null || photoUri == currentPhoto || !photoUri.startsWith(PICKED_IMAGE_SCHEME)) {
                return@withLock user.toProfile(currentPhoto)
            }
            val (updated, newPhoto) = try {
                uploadAvatar(scope, userId, photoUri)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                throw ProfileAvatarUpdateException(user.toProfile(currentPhoto), failure)
            }
            lastProfile = LoadedProfile(scope.key, updated, newPhoto)
            updated.toProfile(newPhoto)
        }
    }

    /** Forgets the cached profile of the signed-out session; its avatar file is removed with the session cache. */
    override suspend fun clearSessionData(): Boolean = withContext(Dispatchers.IO) {
        val scope = tokens.currentSessionScope() ?: return@withContext true
        edits.withLock {
            if (tokens.currentSessionScope()?.key != scope.key) return@withLock false
            lastProfile = null
            clearedSession = scope.key
            true
        }
    }

    private suspend fun updateDetails(
        scope: AuthSessionScope,
        name: String,
        phone: String,
        isPhoneChanged: Boolean
    ): UserResponse = try {
        remote.updateMe(scope, UpdateMyProfileRequest(name, phone.ifBlank { null }))
    } catch (error: HttpException) {
        if (isPhoneChanged && httpFailureKind(error.code()) == HttpFailureKind.VALIDATION) {
            error.response()?.errorBody()?.close()
            throw ProfilePhoneRejectedException(error)
        }
        throw error
    }

    private suspend fun uploadAvatar(scope: AuthSessionScope, userId: String, pickedUri: String): Pair<UserResponse, String> {
        val jpeg = checkNotNull(avatars.encodeForUpload(pickedUri)) { "The selected image cannot be read" }
        val user = remote.uploadAvatar(scope, jpeg)
        checkOwner(scope, userId, user)
        return user to avatars.save(userId, user.updatedAt, jpeg.inputStream())
    }

    /** Returns the avatar as a local file, downloading it once per server version. A missing image is not an error. */
    private suspend fun avatarOf(scope: AuthSessionScope, userId: String, user: UserResponse): String? {
        if (user.avatar == null) {
            avatars.clear(userId)
            return null
        }
        avatars.cached(userId, user.updatedAt)?.let { return it }
        return try {
            remote.avatar(scope)?.use { body ->
                check(tokens.requireUserId(scope) == userId)
                avatars.save(userId, user.updatedAt, body.byteStream())
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Older builds kept the phone number and avatar only on the device. Moves them to the backend once when the
     * account has none there yet, then deletes the device copy. A failure keeps the copy for the next load.
     */
    private suspend fun moveDeviceEdits(scope: AuthSessionScope, userId: String, user: UserResponse): UserResponse {
        val saved = deviceEdits.readEdits(userId)
        if (saved.phone.isBlank() && saved.photoUri == null) return user
        var current = user
        try {
            if (saved.phone.isNotBlank() && current.phone.isNullOrBlank()) {
                current = try {
                    remote.updateMe(scope, UpdateMyProfileRequest(current.name, saved.phone))
                } catch (error: HttpException) {
                    // The backend rejects numbers the old device-only form accepted; such a number cannot move.
                    if (httpFailureKind(error.code()) != HttpFailureKind.VALIDATION) throw error
                    error.response()?.errorBody()?.close()
                    current
                }
                checkOwner(scope, userId, current)
            }
            val photoUri = saved.photoUri
            if (photoUri != null && current.avatar == null) {
                // An image the app can no longer read (deleted, permission revoked) is dropped.
                avatars.encodeForUpload(photoUri)?.let { jpeg ->
                    current = remote.uploadAvatar(scope, jpeg)
                    checkOwner(scope, userId, current)
                }
            }
            deviceEdits.clearEdits(userId)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            return current
        }
        return current
    }

    private fun checkOwner(scope: AuthSessionScope, userId: String, user: UserResponse) {
        check(tokens.requireUserId(scope) == userId && user.id.toString() == userId) { "Session changed" }
    }

    private companion object {
        const val PICKED_IMAGE_SCHEME = "content:"
    }
}
