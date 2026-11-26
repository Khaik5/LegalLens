package com.example.lagallens.data.mapper

import com.example.lagallens.data.model.response.AnalysisCitationResponse
import com.example.lagallens.data.model.response.AnalysisFindingResponse
import com.example.lagallens.data.model.response.AnalysisResultResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AnalysisResultMapperTest {
    private val nbsp = '\u00A0'

    private fun detail(finding: AnalysisFindingResponse, citation: AnalysisCitationResponse? = null) = AnalysisResultMapper.toDetail(
        AnalysisResultResponse(
            jobId = "j1", contractId = 1, documentId = null, overview = null, summarySections = null, keyFacts = null,
            parties = null, obligations = null, findings = listOf(finding), citations = listOfNotNull(citation), access = null
        ),
        AnalysisObligationLabels("A", "B", "C")
    )

    @Test fun unbreakableSpacesInAFindingAreShownAsPlainSpaces() {
        val finding = AnalysisFindingResponse(
            id = "f1", severity = "HIGH", clause = "Điều 5", title = "Chấm dứt hợp đồng${nbsp}ngay",
            description = "hợp đồng${nbsp}bất kỳ", consequence = "đột${nbsp}ngột", explanation = "báo${nbsp}trước",
            recommendation = "sửa${nbsp}đổi", clauseExcerpt = "Điều 5. hợp đồng${nbsp}bất kỳ lúc nào"
        )

        val result = detail(finding).findings.single()

        assertEquals("Điều 5. hợp đồng bất kỳ lúc nào", result.clauseExcerpt)
        assertEquals("hợp đồng bất kỳ", result.description)
        assertEquals("báo trước", result.explanation)
        assertEquals("sửa đổi", result.recommendation)
        assertFalse(listOf(result.title, result.consequence).any { it.contains(nbsp) })
    }

    @Test fun unbreakableSpacesInACitedArticleAreShownAsPlainSpaces() {
        val citation = AnalysisCitationResponse("c1", 7, "Bộ luật Lao động", "25", "Thời gian thử việc", null, "không quá${nbsp}60 ngày", null, null)
        val finding = AnalysisFindingResponse("f1", "HIGH", "", "", "", "", null, null)

        assertEquals("không quá 60 ngày", detail(finding, citation).citations.single().excerpt)
    }
}
