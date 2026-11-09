package com.example.lagallens

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lagallens.data.datasource.local.datastore.AccountStorageName
import com.example.lagallens.data.datasource.local.datastore.AtomicFinanceStorage
import com.example.lagallens.data.repository.FinanceSnapshot
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AtomicFinanceStorageDeviceTest {
    @Test fun fileRoundTripRestartAndInterruptedWrite() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        check(context.packageName == "com.example.lagallens.testsandbox")
        val directory = File(context.cacheDir, "test_finance_${UUID.randomUUID()}")
        check(directory.parentFile?.canonicalFile == context.cacheDir.canonicalFile && !directory.exists())
        val account = UUID.randomUUID().toString()
        val storage = AtomicFinanceStorage(directory)
        val state = FinanceSnapshot()
        val base = File(directory, AccountStorageName.forUser("finance", account))
        try {
            storage.write(account, state)
            File(base.path + ".new").writeText("interrupted snapshot")
            assertEquals(state, AtomicFinanceStorage(directory).read(account))
            base.writeText("{}")
            assertTrue(runCatching { storage.read(account) }.isFailure)
            assertEquals("{}", base.readText())
        } finally {
            listOf(base, File(base.path + ".new"), File(base.path + ".bak")).forEach { file ->
                if (file.exists()) check(file.delete()) { "Could not remove test fixture" }
            }
            if (directory.exists()) check(directory.delete()) { "Test directory is not empty" }
        }
    }
}
