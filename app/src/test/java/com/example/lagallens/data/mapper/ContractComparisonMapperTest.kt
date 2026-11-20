package com.example.lagallens.data.mapper

import com.example.lagallens.data.model.response.ComparedContractResponse
import com.example.lagallens.data.model.response.ComparisonKeyFactResponse
import com.example.lagallens.data.model.response.ComparisonOverviewResponse
import com.example.lagallens.data.model.response.ComparisonOverviewSideResponse
import com.example.lagallens.data.model.response.ComparisonSummaryResponse
import com.example.lagallens.data.model.response.ContractComparisonResponse
import com.example.lagallens.data.model.response.RiskComparisonResponse
import com.example.lagallens.data.model.response.RiskSnapshotResponse
import com.example.lagallens.data.model.response.TextDifferenceResponse
import com.example.lagallens.domain.model.ContractKeyFact
import com.example.lagallens.domain.model.ContractSectionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContractComparisonMapperTest {
    private fun side(id: Long) = ComparedContractResponse(id, id * 10, "HĐ $id", null, null, null, 1L, 1L, "LOW")

    private fun response(
        differences: List<TextDifferenceResponse> = emptyList(),
        risk: RiskComparisonResponse? = null,
        overview: ComparisonOverviewResponse = ComparisonOverviewResponse(emptyList())
    ) = ContractComparisonResponse(side(1), side(2), ComparisonSummaryResponse(1, 0, 0, 0), differences, risk, overview)

    @Test fun clauseKindSimilarityAndRenumberingAreKept() {
        val mapped = response(listOf(
            TextDifferenceResponse("Nội dung", "a", "b", "MODIFIED", kind = "PREAMBLE", matchedBy = "NUMBER", similarity = 0.45),
            TextDifferenceResponse("Điều 5", "x", "y", "MODIFIED", kind = "ARTICLE", matchedBy = "TITLE", similarity = 1.4),
            TextDifferenceResponse("Điều 9", "", "z", "ADDED")
        )).toComparison().differences

        assertEquals(ContractSectionKind.PREAMBLE, mapped[0].kind)
        assertEquals(0.45, mapped[0].similarity!!, 0.0)
        assertFalse(mapped[0].isRenumbered)
        assertTrue(mapped[1].isRenumbered)
        assertEquals(1.0, mapped[1].similarity!!, 0.0)
        assertNull(mapped[2].kind)
        assertNull(mapped[2].similarity)
    }

    @Test fun aRiskWithoutScoresStillShowsTheLevelsAndTheSourceCheck() {
        val risk = response(risk = RiskComparisonResponse(
            RiskSnapshotResponse(null, "HIGH", matchesSource = false), RiskSnapshotResponse(10.0, "LOW", matchesSource = true), null
        )).toComparison().riskComparison!!

        assertNull(risk.first.score)
        assertEquals(false, risk.first.matchesSource)
        assertNull(risk.scoreDelta)
    }

    @Test fun keyFactsOfBothSidesAreMappedAndAPreviewIsFlagged() {
        val comparison = response(overview = ComparisonOverviewResponse(
            emptyList(),
            ComparisonOverviewSideResponse(listOf(ComparisonKeyFactResponse("Mức lương", "15 triệu", "Điều 4"), ComparisonKeyFactResponse(" ", "x", null)), "FULL"),
            ComparisonOverviewSideResponse(listOf(ComparisonKeyFactResponse("Mức lương", "20 triệu", null)), "PREVIEW")
        )).toComparison()

        assertEquals(listOf(ContractKeyFact("Mức lương", "15 triệu", "Điều 4")), comparison.firstKeyFacts)
        assertEquals(listOf(ContractKeyFact("Mức lương", "20 triệu", null)), comparison.secondKeyFacts)
        assertTrue(comparison.isKeyFactsPreview)
    }
}
