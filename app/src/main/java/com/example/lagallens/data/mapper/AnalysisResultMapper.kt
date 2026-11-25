package com.example.lagallens.data.mapper

import com.example.lagallens.core.utils.toReadableText
import com.example.lagallens.data.model.response.AnalysisFindingResponse
import com.example.lagallens.data.model.response.AnalysisObligationResponse
import com.example.lagallens.data.model.response.AnalysisOverviewResponse
import com.example.lagallens.data.model.response.AnalysisResultResponse
import com.example.lagallens.data.model.response.AnalysisSourceResponse
import com.example.lagallens.domain.model.analysis.AnalysisCitation
import com.example.lagallens.domain.model.analysis.AnalysisDetail
import com.example.lagallens.domain.model.analysis.AnalysisFinding
import com.example.lagallens.domain.model.analysis.AnalysisKeyFactEntry
import com.example.lagallens.domain.model.analysis.AnalysisObligationEntry
import com.example.lagallens.domain.model.analysis.AnalysisObligationTab
import com.example.lagallens.domain.model.analysis.AnalysisOverview
import com.example.lagallens.domain.model.analysis.AnalysisPartyEntry
import com.example.lagallens.domain.model.analysis.AnalysisPartyFieldEntry
import com.example.lagallens.domain.model.analysis.AnalysisSeverity
import com.example.lagallens.domain.model.analysis.AnalysisSourceExcerpt
import com.example.lagallens.domain.model.analysis.AnalysisSummaryEntry

/** Titles of the obligation groups, which the backend does not send. */
class AnalysisObligationLabels(
    val employer: String,
    val employee: String,
    val other: String,
    /** Group titles of the rights tab; they default to the duties titles when a caller has no separate text. */
    val employerRights: String = employer,
    val employeeRights: String = employee
)

/**
 * Shape-only conversion of a backend analysis result.
 *
 * A locked result only contains the first part of each list. The screens cut every list to a share of its
 * length, so each list is padded with empty entries up to its real size; they carry no text and the
 * screens never show them, they only keep the "N more items locked" count correct.
 */
object AnalysisResultMapper {
    fun toOverview(response: AnalysisResultResponse): AnalysisOverview {
        val overview = response.overview ?: AnalysisOverviewResponse(null, null, null, null, null)
        return AnalysisOverview(
            score = overview.score ?: 0,
            keyFactsCount = overview.keyFactsCount ?: response.keyFacts.orEmpty().size,
            riskCount = overview.riskCount ?: response.findings.orEmpty().size,
            referenceCount = overview.referenceCount ?: response.citations.orEmpty().size,
            severity = overview.overallRiskLevel?.let(::severity)
        )
    }

    fun toDetail(response: AnalysisResultResponse, labels: AnalysisObligationLabels): AnalysisDetail {
        val locked = response.access?.lockedCounts.orEmpty()
        fun hidden(key: String) = (locked[key] ?: 0).coerceAtLeast(0)
        return AnalysisDetail(
            summary = response.summarySections.orEmpty().mapIndexed { index, item ->
                AnalysisSummaryEntry(item.id ?: "s${index + 1}", item.title.orEmpty(), item.description.orEmpty())
            } + List(hidden("summarySections")) { AnalysisSummaryEntry("locked-summary-$it", "", "") },
            keyFacts = response.keyFacts.orEmpty().mapIndexed { index, item ->
                AnalysisKeyFactEntry(
                    item.id ?: "k${index + 1}", item.label.orEmpty(), item.reference.orEmpty(),
                    item.value.orEmpty(), item.description.orEmpty(), item.source?.let(::source)
                )
            } + List(hidden("keyFacts")) { AnalysisKeyFactEntry("locked-key-fact-$it", "", "", "", "") },
            parties = response.parties.orEmpty().mapIndexed { index, item ->
                AnalysisPartyEntry(
                    item.id ?: "p${index + 1}", item.title.orEmpty(), item.role.orEmpty(),
                    item.fields.orEmpty().mapIndexed { fieldIndex, field ->
                        AnalysisPartyFieldEntry(field.id ?: "f${fieldIndex + 1}", field.label.orEmpty(), field.value.orEmpty())
                    }
                )
            } + List(hidden("parties")) { AnalysisPartyEntry("locked-party-$it", "", "", emptyList()) },
            obligations = obligations(response.obligations.orEmpty(), hidden("obligations"), labels),
            findings = response.findings.orEmpty().mapIndexed { index, item -> finding(item, index) } +
                List(hidden("findings")) { AnalysisFinding("locked-finding-$it", AnalysisSeverity.LOW, "", "", "", "") },
            citations = response.citations.orEmpty().mapIndexed { index, item ->
                AnalysisCitation(
                    item.id ?: "c${index + 1}", item.legalArticleId, item.documentTitle?.takeIf(String::isNotBlank),
                    item.articleNumber?.takeIf(String::isNotBlank), item.title.orEmpty(), item.topic?.takeIf(String::isNotBlank),
                    item.excerpt.orEmpty().toReadableText(), item.effectiveDate?.takeIf(String::isNotBlank), item.validityStatus?.takeIf(String::isNotBlank)
                )
            } + List(hidden("citations")) { AnalysisCitation("locked-citation-$it", null, null, null, "", null, "", null, null) }
        )
    }

    /** The backend uses five levels, the app three. */
    fun severity(level: String?): AnalysisSeverity? = when (level?.uppercase()) {
        "CRITICAL", "HIGH" -> AnalysisSeverity.HIGH
        "MEDIUM" -> AnalysisSeverity.MEDIUM
        "LOW", "VERY_LOW" -> AnalysisSeverity.LOW
        else -> null
    }

    private fun finding(item: AnalysisFindingResponse, index: Int) = AnalysisFinding(
        id = item.id ?: "f${index + 1}",
        severity = severity(item.severity) ?: AnalysisSeverity.MEDIUM,
        clause = item.clause.orEmpty(),
        title = item.title.orEmpty().toReadableText(),
        description = item.description.orEmpty().toReadableText(),
        consequence = item.consequence.orEmpty().toReadableText(),
        explanation = item.explanation?.toReadableText(),
        recommendation = item.recommendation?.toReadableText(),
        isLegalBasisVerified = item.legalBasisVerified,
        clauseExcerpt = item.clauseExcerpt?.toReadableText()?.takeIf(String::isNotBlank),
        citationIds = item.citationIds.orEmpty()
    )

    private fun source(source: AnalysisSourceResponse) =
        AnalysisSourceExcerpt(source.reference.orEmpty(), source.title.orEmpty(), source.excerpt.orEmpty().toReadableText())

    private fun obligations(
        items: List<AnalysisObligationResponse>,
        hiddenCount: Int,
        labels: AnalysisObligationLabels
    ): List<AnalysisObligationEntry> {
        fun tab(item: AnalysisObligationResponse) =
            if (item.kind.equals("DUTY", ignoreCase = true)) AnalysisObligationTab.DUTIES else AnalysisObligationTab.RIGHTS
        // Hidden items follow the shown ones in the backend order, so they are counted in the last shown tab.
        val hiddenTab = items.lastOrNull()?.let(::tab) ?: AnalysisObligationTab.RIGHTS
        val result = mutableListOf<AnalysisObligationEntry>()
        AnalysisObligationTab.entries.forEach { currentTab ->
            val inTab = items.withIndex().filter { tab(it.value) == currentTab }
            inTab.groupBy { it.value.party?.uppercase() }.forEach { (party, group) ->
                result += AnalysisObligationEntry.Section(
                    "section-${currentTab.name.lowercase()}-${party.orEmpty().lowercase()}",
                    when (party) {
                        "EMPLOYER" -> if (currentTab == AnalysisObligationTab.RIGHTS) labels.employerRights else labels.employer
                        "EMPLOYEE" -> if (currentTab == AnalysisObligationTab.RIGHTS) labels.employeeRights else labels.employee
                        else -> labels.other
                    },
                    currentTab
                )
                group.forEach { (index, item) ->
                    result += AnalysisObligationEntry.Item(
                        item.id ?: "o${index + 1}", item.title.orEmpty(), item.clause.orEmpty(),
                        item.description.orEmpty(), item.source?.let(::source)
                    )
                }
            }
            if (currentTab == hiddenTab) repeat(hiddenCount) { result += AnalysisObligationEntry.Item("locked-obligation-$it", "", "", "") }
        }
        return result
    }
}
