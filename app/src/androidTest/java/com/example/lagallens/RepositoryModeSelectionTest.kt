package com.example.lagallens

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lagallens.data.repository.RemoteContractRepository
import com.example.lagallens.data.repository.RemoteDocumentRepository
import com.example.lagallens.data.repository.RemoteDocumentUploadRepository
import com.example.lagallens.di.AppDataMode
import com.example.lagallens.di.DataModeConfig
import com.example.lagallens.di.RepositoryModule
import org.junit.Assert.assertFalse
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
        val uploads = RepositoryModule.provideDocumentUploadRepository(context, contracts)

        when (DataModeConfig.current) {
            AppDataMode.DEMO -> {
                assertFalse(contracts is RemoteContractRepository)
                assertFalse(documents is RemoteDocumentRepository)
                assertFalse(uploads is RemoteDocumentUploadRepository)
            }
            AppDataMode.REAL -> {
                assertTrue(contracts is RemoteContractRepository)
                assertTrue(documents is RemoteDocumentRepository)
                assertTrue(uploads is RemoteDocumentUploadRepository)
            }
        }
    }
}
