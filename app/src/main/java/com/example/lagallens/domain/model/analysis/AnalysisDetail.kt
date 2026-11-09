package com.example.lagallens.domain.model.analysis

/** Typed analysis content in the requested language, independent of how it is stored or transported. */
data class AnalysisDetail(
    val summary: List<AnalysisSummaryEntry>,
    val keyFacts: List<AnalysisKeyFactEntry>,
    val parties: List<AnalysisPartyEntry>,
    val obligations: List<AnalysisObligationEntry>,
    val findings: List<AnalysisFinding>
)

data class AnalysisSummaryEntry(val id: String, val title: String, val description: String)

data class AnalysisKeyFactEntry(
    val id: String,
    val label: String,
    val reference: String,
    val value: String,
    val description: String,
    val source: AnalysisSourceExcerpt? = null
)

data class AnalysisSourceExcerpt(val reference: String, val title: String, val excerpt: String)

data class AnalysisPartyEntry(val id: String, val title: String, val role: String, val fields: List<AnalysisPartyFieldEntry>)

data class AnalysisPartyFieldEntry(val id: String, val label: String, val value: String)

enum class AnalysisObligationTab { RIGHTS, DUTIES }

sealed interface AnalysisObligationEntry {
    data class Section(val id: String, val title: String, val tab: AnalysisObligationTab = AnalysisObligationTab.RIGHTS) : AnalysisObligationEntry
    data class Item(val id: String, val title: String, val clause: String, val description: String, val source: AnalysisSourceExcerpt? = null) : AnalysisObligationEntry
}

enum class AnalysisSeverity { HIGH, MEDIUM, LOW }

data class AnalysisFinding(
    val id: String,
    val severity: AnalysisSeverity,
    val clause: String,
    val title: String,
    val description: String,
    val consequence: String,
    val explanation: String? = null,
    val recommendation: String? = null
)
