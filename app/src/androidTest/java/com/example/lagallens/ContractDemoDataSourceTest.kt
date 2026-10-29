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
        val preferences = context.getSharedPreferences("contract_demo_data", android.content.Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        try {
            ContractDemoDataSource.initialize(context)
            assertTrue(ContractDemoDataSource.rename("office-lease", "Hợp đồng đã đổi tên"))
            assertEquals("Hợp đồng đã đổi tên", ContractDemoDataSource.getContract("office-lease")?.titleOverride)
            assertTrue(ContractDemoDataSource.delete("collaborator"))
            assertNull(ContractDemoDataSource.getContract("collaborator"))

            ContractDemoDataSource.initialize(context)

            assertEquals("Hợp đồng đã đổi tên", ContractDemoDataSource.getContract("office-lease")?.titleOverride)
            assertFalse(ContractDemoDataSource.observableContracts.value.any { it.id == "collaborator" })
        } finally {
            preferences.edit().clear().commit()
            ContractDemoDataSource.initialize(context)
        }
    }
}
