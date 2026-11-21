package com.example.lagallens.data.mapper

import com.example.lagallens.data.model.response.ContractResponse
import com.example.lagallens.domain.model.EmploymentContractSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContractMapperSourceTest {
    private fun response(source: String? = null, reviewRequired: Boolean? = null) = ContractResponse(
        id = 1, title = "A", status = "DRAFT", description = null,
        createdAt = "2026-11-10T09:30:00.000Z", updatedAt = "2026-11-10T09:30:00.000Z",
        source = source, reviewRequired = reviewRequired
    )

    @Test fun anAiDraftedContractKeepsItsSourceAndReviewFlag() {
        val contract = response("AI_GENERATED", true).toContract()
        assertEquals(EmploymentContractSource.AI_GENERATED, contract.source)
        assertEquals(true, contract.reviewRequired)
    }

    @Test fun anUploadedContractIsNeverMarkedForReview() {
        val contract = response("UPLOADED", false).toContract()
        assertEquals(EmploymentContractSource.UPLOADED, contract.source)
        assertEquals(false, contract.reviewRequired)
    }

    @Test fun aBackendThatDoesNotSendThemIsTreatedAsUploadedWithoutAReviewFlag() {
        val contract = response().toContract()
        assertEquals(EmploymentContractSource.UPLOADED, contract.source)
        assertNull(contract.reviewRequired)
    }

    @Test fun anUnknownSourceFallsBackToUploaded() {
        assertEquals(EmploymentContractSource.UPLOADED, response("SOMETHING_NEW").toContract().source)
    }
}
