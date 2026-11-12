package com.example.lagallens.data.repository

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.datasource.local.datastore.ProfileSettingsStore
import com.example.lagallens.data.datasource.remote.ProfilePreferencesRemoteDataSource
import com.example.lagallens.data.datasource.remote.api.ProfilePreferencesApi
import com.example.lagallens.data.model.request.PatchProfilePreferencesRequest
import com.example.lagallens.data.model.response.ApiEnvelope
import com.example.lagallens.data.model.response.ProfilePreferencesResponse
import com.example.lagallens.domain.model.ProfileAppearance
import com.example.lagallens.domain.model.ProfileLanguage
import com.example.lagallens.domain.model.ProfileSettings
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteProfileSettingsRepositoryTest {
    private val session = RemoteRepositorySession()
    private val local = StoreFixture()
    private val api = PreferencesFixture()
    private val repository = RemoteProfileSettingsRepository(local, session, ProfilePreferencesRemoteDataSource(lazyOf(api)))

    @Test fun changingTheLanguageAppliesLocallyAndSendsOnlyThatField() = runTest {
        repository.updateLanguage(ProfileLanguage.ENGLISH)

        assertEquals(ProfileLanguage.ENGLISH, local.settings.value.language)
        assertEquals(listOf(PatchProfilePreferencesRequest(language = "ENGLISH")), api.patches)
        assertNull(local.pending)
    }

    @Test fun aFailedSendStaysPendingForThisAccount() = runTest {
        api.patchFailure = IOException("offline")

        repository.updateNotificationPreferences(isPushEnabled = false, isEmailEnabled = true, isInAppEnabled = true)

        assertEquals(false, local.settings.value.isPushEnabled)
        assertEquals("42", local.pending)
    }

    @Test fun theNextChangeResendsTheEarlierOneToo() = runTest {
        api.patchFailure = IOException("offline")
        repository.updateLanguage(ProfileLanguage.ENGLISH)
        api.patchFailure = null

        repository.updateAppearance(ProfileAppearance.LIGHT)

        assertEquals(local.settings.value.toRequest(), api.patches.single())
        assertNull(local.pending)
    }

    @Test fun syncSendsPendingChangesThenTakesTheServerCopy() = runTest {
        local.settings.value = ProfileSettings(language = ProfileLanguage.ENGLISH)
        local.pending = "42"
        api.server = ProfilePreferencesResponse("LIGHT", "ENGLISH", false, true, true)

        repository.syncFromServer()

        assertEquals(listOf(ProfileSettings(language = ProfileLanguage.ENGLISH).toRequest()), api.patches)
        assertEquals(ProfileSettings(ProfileAppearance.LIGHT, ProfileLanguage.ENGLISH, false, true, true), local.settings.value)
        assertNull(local.pending)
    }

    @Test fun anotherAccountsPendingChangeIsNeverSent() = runTest {
        local.settings.value = ProfileSettings(language = ProfileLanguage.ENGLISH)
        local.pending = "7"
        api.server = ProfilePreferencesResponse("SYSTEM", "VIETNAMESE", true, true, false)

        repository.syncFromServer()

        assertTrue(api.patches.isEmpty())
        assertEquals(ProfileLanguage.VIETNAMESE, local.settings.value.language)
        assertNull(local.pending)
    }

    @Test fun unknownServerValuesFallBackToDefaults() = runTest {
        api.server = ProfilePreferencesResponse("PURPLE", null, null, null, null)

        repository.syncFromServer()

        assertEquals(ProfileSettings(), local.settings.value)
    }

    private fun ProfileSettings.toRequest() =
        PatchProfilePreferencesRequest(appearance.name, language.name, isPushEnabled, isEmailEnabled, isInAppEnabled)

    private class StoreFixture : ProfileSettingsStore {
        val settings = MutableStateFlow(ProfileSettings())
        var pending: String? = null

        override fun observeSettings(): Flow<ProfileSettings> = settings
        override suspend fun updateAppearance(appearance: ProfileAppearance) = settings.update { it.copy(appearance = appearance) }
        override suspend fun updateLanguage(language: ProfileLanguage) = settings.update { it.copy(language = language) }
        override suspend fun updateNotificationPreferences(isPushEnabled: Boolean, isEmailEnabled: Boolean, isInAppEnabled: Boolean) =
            settings.update { it.copy(isPushEnabled = isPushEnabled, isEmailEnabled = isEmailEnabled, isInAppEnabled = isInAppEnabled) }
        override suspend fun replace(settings: ProfileSettings) { this.settings.value = settings }
        override suspend fun pendingOwner() = pending
        override suspend fun markPending(ownerId: String) { pending = ownerId }
        override suspend fun clearPending() { pending = null }
    }

    private class PreferencesFixture : ProfilePreferencesApi {
        var server = ProfilePreferencesResponse("SYSTEM", "VIETNAMESE", true, true, false)
        var patchFailure: Exception? = null
        val patches = mutableListOf<PatchProfilePreferencesRequest>()

        override suspend fun get(scope: AuthSessionScope) = ApiEnvelope(true, server)

        override suspend fun patch(scope: AuthSessionScope, request: PatchProfilePreferencesRequest): ApiEnvelope<ProfilePreferencesResponse> {
            patchFailure?.let { throw it }
            patches += request
            return ApiEnvelope(true, server)
        }
    }
}
