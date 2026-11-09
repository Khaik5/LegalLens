package com.example.lagallens

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lagallens.data.datasource.local.datastore.ContractDemoDataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContractDemoDataSourceTest {
    @Test fun renameAndDeletePersistAcrossDataSourceRefresh() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "test_contract_${java.util.UUID.randomUUID()}"
        val source = ContractDemoDataSource(name)
        try {
            source.initialize(context)
            assertTrue(source.rename("office-lease", "Hợp đồng đã đổi tên"))
            assertEquals("Hợp đồng đã đổi tên", source.getContract("office-lease")?.titleOverride)
            assertTrue(source.delete("collaborator"))
            assertNull(source.getContract("collaborator"))

            val reopened = ContractDemoDataSource(name)
            reopened.initialize(context)

            assertEquals("Hợp đồng đã đổi tên", reopened.getContract("office-lease")?.titleOverride)
            assertFalse(reopened.observableContracts.value.any { it.id == "collaborator" })
        } finally {
            context.deleteSharedPreferences(name)
        }
    }
}
