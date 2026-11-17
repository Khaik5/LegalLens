package com.example.lagallens.presentation.feature.analysis.legalreferences.model

import android.content.Context
import com.example.lagallens.R

/** Display text for backend legal references, whose metadata is often missing. */
object LegalReferenceFormat {
    /** A bare number reads "Điều 301"; text that already names the article is kept. */
    fun article(context: Context, articleNumber: String): String =
        if (articleNumber.all(Char::isDigit)) context.getString(R.string.chatbot_legal_reference_article, articleNumber) else articleNumber

    fun effective(context: Context, effectiveDate: String?): String =
        effectiveDate?.let { context.getString(R.string.analysis_legal_references_effective_from, it) }
            ?: context.getString(R.string.analysis_legal_references_effective_unknown)

    fun status(context: Context, validityStatus: String?): String = when (validityStatus?.uppercase()) {
        "ACTIVE", "EFFECTIVE", "IN_FORCE", "VALID" -> context.getString(R.string.analysis_legal_reference_detail_status_active)
        null -> context.getString(R.string.analysis_legal_reference_unknown_value)
        else -> validityStatus
    }
}
