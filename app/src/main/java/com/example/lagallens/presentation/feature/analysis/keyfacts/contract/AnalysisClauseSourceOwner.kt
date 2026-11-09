package com.example.lagallens.presentation.feature.analysis.keyfacts.contract

interface AnalysisClauseSourceOwner {
    fun isVerifiedClause(source: AnalysisClauseSource): Boolean
}
