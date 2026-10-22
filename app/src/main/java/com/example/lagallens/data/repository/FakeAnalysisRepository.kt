package com.example.lagallens.data.repository

import com.example.lagallens.domain.model.analysis.AnalysisOverview
import com.example.lagallens.domain.repository.AnalysisRepository

/** Development-only analysis summary source; replace with the backend analysis repository when approved. */
class FakeAnalysisRepository : AnalysisRepository {
    override suspend fun getOverview(documentName: String, documentMetadata: String): AnalysisOverview =
        AnalysisOverview(
            score = 65,
            keyFactsCount = 8,
            riskCount = 3,
            referenceCount = 12
        )
}
