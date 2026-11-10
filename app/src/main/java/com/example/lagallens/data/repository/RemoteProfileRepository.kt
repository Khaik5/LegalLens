package com.example.lagallens.data.repository

import com.example.lagallens.data.datasource.local.datastore.AuthSessionStore
import com.example.lagallens.data.datasource.local.datastore.ProfileLocalEdits
import com.example.lagallens.data.datasource.local.datastore.currentSessionScope
import com.example.lagallens.data.datasource.local.datastore.requireUserId
import com.example.lagallens.data.datasource.remote.UserRemoteDataSource
import com.example.lagallens.data.mapper.toProfile
import com.example.lagallens.data.model.response.UserResponse
import com.example.lagallens.domain.repository.ProfileNameUpdateException
import com.example.lagallens.domain.repository.ProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class RemoteProfileRepository internal constructor(
    private val tokens: AuthSessionStore,
    private val remote: UserRemoteDataSource,
    private val local: ProfileLocalEdits
) : ProfileRepository {
    private val edits = Mutex()
    private var lastProfile: Pair<String, UserResponse>? = null
    private var clearedSession: String? = null

    override suspend fun getProfile() = withContext(Dispatchers.IO) {
        val scope = checkNotNull(tokens.currentSessionScope()) { "No active session" }
        val userId = tokens.requireUserId(scope)
        edits.withLock {
            val user = remote.me(scope)
            check(tokens.requireUserId(scope) == userId && user.id.toString() == userId)
            lastProfile = scope.key to user
            val details = local.readEdits(userId)
            user.toProfile(details.phone, details.photoUri)
        }
    }

    override suspend fun saveProfile(name: String, phone: String, photoUri: String?) = withContext(Dispatchers.IO) {
        val scope = checkNotNull(tokens.currentSessionScope()) { "No active session" }
        val userId = tokens.requireUserId(scope)
        edits.withLock {
            check(tokens.requireUserId(scope) == userId && clearedSession != scope.key)
            val current = lastProfile?.takeIf { it.first == scope.key }?.second ?: remote.me(scope)
            check(tokens.requireUserId(scope) == userId && current.id.toString() == userId)
            val details = local.saveEdits(userId, phone.trim(), photoUri)
            lastProfile = scope.key to current
            val trimmedName = name.trim()
            val user = if (current.name == trimmedName) current else try {
                remote.update(scope, userId, trimmedName)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                check(tokens.requireUserId(scope) == userId)
                throw ProfileNameUpdateException(current.toProfile(details.phone, details.photoUri), failure)
            }
            check(tokens.requireUserId(scope) == userId && user.id.toString() == userId)
            lastProfile = scope.key to user
            user.toProfile(details.phone, details.photoUri)
        }
    }

    /**
     * Forgets the cached profile of the signed-out session. The phone number and avatar stay on the device:
     * the backend does not store them, and they are kept per account, so another account never sees them.
     */
    override suspend fun clearSessionData(): Boolean = withContext(Dispatchers.IO) {
        val scope = tokens.currentSessionScope() ?: return@withContext true
        edits.withLock {
            if (tokens.currentSessionScope()?.key != scope.key) return@withLock false
            lastProfile = null
            clearedSession = scope.key
            true
        }
    }
}
