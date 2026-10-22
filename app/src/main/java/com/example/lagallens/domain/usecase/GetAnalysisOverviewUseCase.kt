package com.example.lagallens.domain.usecase

import com.example.lagallens.domain.model.analysis.AnalysisOverview
import com.example.lagallens.domain.repository.AnalysisRepository

class GetAnalysisOverviewUseCase(
    private val repository: AnalysisRepository
) {
    suspend operator fun invoke(documentName: String, documentMetadata: String): AnalysisOverview =
        repository.getOverview(documentName, documentMetadata)
}
