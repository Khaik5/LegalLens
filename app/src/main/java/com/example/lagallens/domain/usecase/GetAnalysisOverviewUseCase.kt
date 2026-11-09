package com.example.lagallens.domain.usecase

import com.example.lagallens.domain.model.analysis.AnalysisOverview
import com.example.lagallens.domain.repository.AnalysisRepository

class GetAnalysisOverviewUseCase(
    private val repository: AnalysisRepository
) {
    suspend operator fun invoke(identity: com.example.lagallens.domain.model.analysis.AnalysisIdentity): AnalysisOverview =
        repository.getOverview(identity)
}
