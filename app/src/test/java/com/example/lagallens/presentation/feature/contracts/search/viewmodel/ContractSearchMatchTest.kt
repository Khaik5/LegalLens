package com.example.lagallens.presentation.feature.contracts.search.viewmodel

import com.example.lagallens.presentation.feature.contracts.search.contract.ContractSearchResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContractSearchMatchTest {
    private fun result(vararg keywords: String) =
        ContractSearchResult.samples.first().copy(titleOverride = "Hợp đồng A", searchKeywords = keywords.toList())

    @Test fun emptyContentDoesNotMatchEveryQuery() {
        assertFalse(result("").matchesQuery("không tồn tại"))
    }

    @Test fun queryLongerThanAKeywordDoesNotMatch() {
        assertFalse(result("lao động").matchesQuery("lao động thời vụ"))
    }

    @Test fun contentMatchIgnoresCase() {
        assertTrue(result("ĐIỀU 1. CÔNG VIỆC").matchesQuery("công việc"))
    }

    @Test fun blankQueryKeepsEveryResult() {
        assertTrue(result("").matchesQuery("  "))
    }
}
