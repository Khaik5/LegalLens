package com.example.lagallens.data.repository

import com.example.lagallens.data.datasource.local.datastore.AuthSessionStore
import com.example.lagallens.data.datasource.local.datastore.ProfileSettingsStore
import com.example.lagallens.data.datasource.local.datastore.currentSessionScope
import com.example.lagallens.data.datasource.local.datastore.requireUserId
import com.example.lagallens.data.datasource.remote.ProfilePreferencesRemoteDataSource
import com.example.lagallens.data.mapper.toPatchRequest
import com.example.lagallens.data.mapper.toProfileSettings
import com.example.lagallens.data.model.request.PatchProfilePreferencesRequest
import com.example.lagallens.domain.model.ProfileAppearance
import com.example.lagallens.domain.model.ProfileLanguage
import com.example.lagallens.domain.model.ProfileSettings
import com.example.lagallens.domain.repository.ProfileSettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Settings follow the account. A change is applied on the device first, so the language switches at once, and is
 * then sent to the backend. A change the backend did not receive is remembered for that account and sent on the
 * next sync; it is never sent to a different account.
 */
class RemoteProfileSettingsRepository(
    private val local: ProfileSettingsStore,
    private val tokens: AuthSessionStore,
    private val remote: ProfilePreferencesRemoteDataSource
) : ProfileSettingsRepository {
    private val lock = Mutex()

    override fun observeSettings(): Flow<ProfileSettings> = local.observeSettings()

    override suspend fun updateAppearance(appearance: ProfileAppearance) {
        local.updateAppearance(appearance)
        send(PatchProfilePreferencesRequest(appearance = appearance.name))
    }

    override suspend fun updateLanguage(language: ProfileLanguage) {
        local.updateLanguage(language)
        send(PatchProfilePreferencesRequest(language = language.name))
    }

    override suspend fun updateNotificationPreferences(isPushEnabled: Boolean, isEmailEnabled: Boolean, isInAppEnabled: Boolean) {
        local.updateNotificationPreferences(isPushEnabled, isEmailEnabled, isInAppEnabled)
        send(PatchProfilePreferencesRequest(isPushEnabled = isPushEnabled, isEmailEnabled = isEmailEnabled, isInAppEnabled = isInAppEnabled))
    }

    /** Sends any change still waiting for this account, then takes the backend copy as the device copy. */
    override suspend fun syncFromServer() = withContext(Dispatchers.IO) {
        val scope = checkNotNull(tokens.currentSessionScope()) { "No active session" }
        val userId = tokens.requireUserId(scope)
        lock.withLock {
            when (local.pendingOwner()) {
                null -> Unit
                userId -> {
                    remote.patch(scope, local.observeSettings().first().toPatchRequest())
                    local.clearPending()
                }
                // Left by a previous account on this device; it must not reach this one.
                else -> local.clearPending()
            }
            val settings = remote.get(scope).toProfileSettings()
            check(tokens.requireUserId(scope) == userId) { "Session changed" }
            local.replace(settings)
        }
    }

    /** Signed out: the change stays on the device only. A failure keeps it pending for the next sync. */
    private suspend fun send(request: PatchProfilePreferencesRequest) = withContext(Dispatchers.IO) {
        val scope = tokens.currentSessionScope() ?: return@withContext
        val userId = tokens.userId() ?: return@withContext
        lock.withLock {
            // An earlier change that never arrived is included, or the next sync would overwrite it.
            val body = if (local.pendingOwner() == userId) local.observeSettings().first().toPatchRequest() else request
            local.markPending(userId)
            try {
                remote.patch(scope, body)
                if (tokens.currentSessionScope()?.key == scope.key) local.clearPending()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // Still marked pending; syncFromServer sends the whole device copy later.
            }
        }
    }
}
