package com.example.lagallens

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lagallens.core.network.BackendNotConfiguredException
import com.example.lagallens.data.repository.UnconfiguredRemoteProfileRepository
import com.example.lagallens.di.AppDataMode
import com.example.lagallens.di.RepositoryModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileRealRepositoryTest {
    @Test fun realModeSelectsUnavailableRemoteAndDoesNotReadOrWriteDemoProfile() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = context.getSharedPreferences("profile_demo_data", Context.MODE_PRIVATE)
        val previous = preferences.all.toMap()
        val realRepository = RepositoryModule.provideProfileRepository(context, { "demo@example.test" }, AppDataMode.REAL)

        assertTrue(realRepository is UnconfiguredRemoteProfileRepository)
        val error = runCatching { realRepository.getProfile() }.exceptionOrNull()
        assertTrue(error is BackendNotConfiguredException)
        assertEquals(previous, preferences.all)
    }
}
