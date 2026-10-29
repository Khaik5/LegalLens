package com.example.lagallens

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lagallens.data.repository.FakeContractRepository
import com.example.lagallens.data.repository.FakeDocumentRepository
import com.example.lagallens.data.repository.UnconfiguredRemoteContractRepository
import com.example.lagallens.data.repository.UnconfiguredRemoteDocumentRepository
import com.example.lagallens.di.AppDataMode
import com.example.lagallens.di.DataModeConfig
import com.example.lagallens.di.RepositoryModule
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoryModeSelectionTest {
    @Test
    fun contractAndDocumentRepositoriesFollowTheBuildMode() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val contracts = RepositoryModule.provideContractRepository(context)
        val documents = RepositoryModule.provideDocumentRepository(context)

        when (DataModeConfig.current) {
            AppDataMode.DEMO -> {
                assertTrue(contracts is FakeContractRepository)
                assertTrue(documents is FakeDocumentRepository)
            }
            AppDataMode.REAL -> {
                assertTrue(contracts is UnconfiguredRemoteContractRepository)
                assertTrue(documents is UnconfiguredRemoteDocumentRepository)
            }
        }
    }
}
