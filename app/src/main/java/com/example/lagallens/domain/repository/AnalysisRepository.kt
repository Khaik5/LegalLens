package com.example.lagallens.domain.repository

import com.example.lagallens.domain.model.analysis.AnalysisOverview

interface AnalysisRepository {
    suspend fun getOverview(documentName: String, documentMetadata: String): AnalysisOverview
}
