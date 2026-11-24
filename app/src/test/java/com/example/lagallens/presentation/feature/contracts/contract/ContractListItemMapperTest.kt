package com.example.lagallens.presentation.feature.contracts.contract

import com.example.lagallens.R
import com.example.lagallens.domain.model.EmploymentContract
import com.example.lagallens.domain.model.EmploymentContractStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ContractListItemMapperTest {
    private fun contract(status: EmploymentContractStatus) = EmploymentContract(
        id = "1",
        documentId = "",
        title = "Hợp đồng",
        content = "",
        createdAtMillis = 0L,
        updatedAtMillis = 0L,
        status = status
    )

    @Test
    fun aContractWaitingForReviewShowsThePendingReviewLabel() {
        val item = contract(EmploymentContractStatus.PENDING_REVIEW).toContractListItem(subtitle = "")

        assertEquals(ContractProcessingStatus.PENDING_REVIEW, item.processingStatus)
        assertEquals(R.string.contract_pending_review_status, item.statusRes)
    }

    @Test
    fun aDraftKeepsTheDraftLabel() {
        val item = contract(EmploymentContractStatus.DRAFT).toContractListItem(subtitle = "")

        assertEquals(R.string.contract_draft_status, item.statusRes)
    }
}
