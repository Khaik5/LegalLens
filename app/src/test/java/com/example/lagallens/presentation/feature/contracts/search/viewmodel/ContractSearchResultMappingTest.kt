package com.example.lagallens.presentation.feature.contracts.search.viewmodel

import com.example.lagallens.domain.model.EmploymentContract
import com.example.lagallens.domain.model.EmploymentContractType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContractSearchResultMappingTest {
    private fun contract(title: String, content: String = "", fileName: String = "", type: EmploymentContractType = EmploymentContractType.EMPLOYMENT) =
        EmploymentContract("1", "d1", title, content, 0, 0, fileName = fileName, type = type)

    @Test fun subtitleIsTheRealContractTypeNotSampleData() {
        val result = contract("HĐ dịch vụ IT", type = EmploymentContractType.SERVICE).toSearchResult("Hợp đồng dịch vụ")

        assertEquals("HĐ dịch vụ IT", result.titleOverride)
        assertEquals("Hợp đồng dịch vụ", result.subtitleOverride)
    }

    @Test fun genericEmploymentWordsNoLongerMatchEveryContract() {
        val service = contract("HĐ dịch vụ IT", type = EmploymentContractType.SERVICE).toSearchResult("Hợp đồng dịch vụ")

        assertFalse(service.matchesQuery("người lao động"))
        assertFalse(service.matchesQuery("hợp đồng lao động"))
    }

    @Test fun matchesItsOwnTitleTextFileNameAndType() {
        val result = contract(
            title = "HĐ Công ty An Phát",
            content = "Điều 3. Mức lương 15 triệu",
            fileName = "an-phat-2026.pdf"
        ).toSearchResult("Hợp đồng lao động")

        assertTrue(result.matchesQuery("an phát"))
        assertTrue(result.matchesQuery("mức lương"))
        assertTrue(result.matchesQuery("an-phat-2026"))
        assertTrue(result.matchesQuery("hợp đồng lao động"))
    }

    @Test fun blankFieldsAreNotKeywords() {
        val result = contract("HĐ A").toSearchResult("Hợp đồng lao động")

        assertEquals(listOf("HĐ A", "Hợp đồng lao động"), result.searchKeywords)
    }
}
