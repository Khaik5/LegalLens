package com.example.lagallens

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lagallens.data.datasource.local.datastore.EmploymentDemoDataSource
import com.example.lagallens.data.repository.FakeContractRepository
import com.example.lagallens.data.repository.FakeDocumentRepository
import com.example.lagallens.domain.model.EmploymentContract
import com.example.lagallens.domain.model.EmploymentContractRisk
import com.example.lagallens.domain.model.EmploymentContractStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EmploymentRepositoryTest {
    @Test
    fun employmentCrudDocumentsCompareAndPersistenceUseSelectedContent() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences("contract_demo_data", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        try {
            val source = EmploymentDemoDataSource(context)
            val contracts = FakeContractRepository(source)
            val documents = FakeDocumentRepository(source)
            val initial = contracts.observeContracts().first()
            assertEquals(4, initial.size)
            assertTrue(initial.all { it.content.contains("ĐIỀU ") })
            assertTrue(initial.none { it.title.contains(Regex("thuê|mua bán|nda|bitexco", RegexOption.IGNORE_CASE)) })

            val now = System.currentTimeMillis()
            val created = contracts.createContract(
                EmploymentContract(
                    id = "demo-created-contract",
                    documentId = "demo-created-document",
                    title = "Hợp đồng lao động DEMO do test tạo",
                    content = "ĐIỀU 1. CÔNG VIỆC\n[Chức danh A]\nĐIỀU 2. THỜI HẠN\n[Theo nội dung đã chọn]",
                    createdAtMillis = now,
                    updatedAtMillis = now,
                    status = EmploymentContractStatus.COMPLETE,
                    risk = EmploymentContractRisk.NOT_ANALYZED,
                    fileName = "sample.pdf",
                    mimeType = "application/pdf"
                )
            )
            assertEquals("demo-created-document", created.documentId)
            assertTrue(contracts.renameContract(created.id, "Tên đã chỉnh sửa"))
            val editedText = "ĐIỀU 1. CÔNG VIỆC\n[Chức danh đã sửa]\nĐIỀU 2. THỜI HẠN\n[Theo nội dung đã chọn]"
            assertTrue(documents.updateDocumentContent(created.documentId, created.id, editedText))

            val recreated = EmploymentDemoDataSource(context)
            val reopenedContracts = FakeContractRepository(recreated)
            val reopenedDocuments = FakeDocumentRepository(recreated)
            val persisted = reopenedContracts.getContract(created.id)
            assertNotNull(persisted)
            assertEquals("Tên đã chỉnh sửa", persisted?.title)
            assertEquals(editedText, persisted?.content)
            assertEquals(editedText, reopenedDocuments.getDocument(created.documentId)?.content)

            val comparison = reopenedContracts.compareContracts(created.id, "collaborator")
            assertNotNull(comparison)
            assertTrue(comparison!!.differences.any { it.firstText.contains("Chức danh đã sửa") })

            assertTrue(reopenedContracts.deleteContract(created.id))
            assertFalse(reopenedContracts.observeContracts().first().any { it.id == created.id })
            assertEquals(null, reopenedDocuments.getDocument(created.documentId))
        } finally {
            preferences.edit().clear().commit()
            EmploymentDemoDataSource(context)
        }
    }
}
