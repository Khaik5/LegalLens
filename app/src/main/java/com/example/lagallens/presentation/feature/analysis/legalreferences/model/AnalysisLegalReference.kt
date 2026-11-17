package com.example.lagallens.presentation.feature.analysis.legalreferences.model

import androidx.annotation.StringRes
import com.example.lagallens.domain.model.analysis.AnalysisCitation

/** Text of a reference that comes from the backend instead of string resources. */
data class AnalysisLegalReferenceText(
    val title: String,
    val articleNumber: String?,
    val topic: String?,
    val description: String,
    val effectiveDate: String?,
    val validityStatus: String?
)

data class AnalysisLegalReference(
    val id: String,
    @StringRes val titleRes: Int = 0,
    @StringRes val countryRes: Int = 0,
    @StringRes val articleRes: Int = 0,
    @StringRes val topicRes: Int = 0,
    @StringRes val descriptionRes: Int = 0,
    @StringRes val effectiveDateRes: Int = 0,
    val searchTerms: String,
    /** Set for backend references; the resource fields above then stay unused. */
    val text: AnalysisLegalReferenceText? = null
)

/** Backend citations in the form the list and detail screens show; hidden placeholder entries are kept so locked counts stay right. */
fun AnalysisCitation.toLegalReference() = AnalysisLegalReference(
    id = id,
    searchTerms = listOfNotNull(documentTitle, articleNumber, title, topic, excerpt).joinToString(" "),
    text = AnalysisLegalReferenceText(documentTitle ?: title, articleNumber, topic ?: title.takeIf { documentTitle != null }, excerpt, effectiveDate, validityStatus)
)
