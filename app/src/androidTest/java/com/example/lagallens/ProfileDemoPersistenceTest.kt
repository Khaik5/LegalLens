package com.example.lagallens

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lagallens.data.datasource.local.datastore.ProfileLocalDataSource
import com.example.lagallens.data.repository.ProfileRepositoryImpl
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileDemoPersistenceTest {
    @Test fun editedProfilePersistsWithoutHardcodedIdentity() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val accountId = java.util.UUID.randomUUID().toString()
        val repository = ProfileRepositoryImpl(ProfileLocalDataSource(context, { accountId }) { "demo.user@example.test" })

        val saved = repository.saveProfile("Demo employee", "0900000000", null)
        val restored = ProfileRepositoryImpl(ProfileLocalDataSource(context, { accountId }) { "demo.user@example.test" }).getProfile()

        assertEquals("Demo employee", saved.name)
        assertEquals("demo.user@example.test", saved.email)
        assertEquals(saved, restored)
    }

    @Test fun logoutClearsProfileDataButKeepsPresentationSettings() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        var accountId: String? = java.util.UUID.randomUUID().toString()
        val originalId = accountId
        val repository = ProfileRepositoryImpl(ProfileLocalDataSource(context, { accountId }) { "demo.user@example.test" })
        repository.saveProfile("Sensitive display name", "0900000000", "content://private/photo")

        assertEquals(true, repository.clearSessionData())
        accountId = null
        val cleared = ProfileRepositoryImpl(ProfileLocalDataSource(context) { null }).getProfile()
        assertEquals(context.getString(com.example.lagallens.R.string.profile_demo_default_name), cleared.name)
        assertEquals("", cleared.phone)
        assertEquals(null, cleared.photoUri)
        accountId = originalId
        assertEquals("Sensitive display name", repository.getProfile().name)
    }
}
