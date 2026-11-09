package com.example.lagallens.domain.usecase

import com.example.lagallens.domain.model.analysis.AnalysisDetail
import com.example.lagallens.domain.model.analysis.AnalysisIdentity
import com.example.lagallens.domain.repository.AnalysisRepository

class GetAnalysisDetailUseCase(private val repository: AnalysisRepository) {
    suspend operator fun invoke(identity: AnalysisIdentity, language: String): AnalysisDetail = repository.getDetail(identity, language)
}
