package com.example.lagallens.presentation.feature.analysis.legalreferences.model

import com.example.lagallens.R

/** Illustrative references only; they are not citations verified for the user's contract. */
object DemoLegalReferences {
    val items = listOf(
        AnalysisLegalReference("commercial-law-2005-article-301", R.string.analysis_legal_references_commercial_law_title, R.string.analysis_legal_references_country_vietnam, R.string.analysis_legal_references_commercial_law_article, R.string.analysis_legal_references_commercial_law_topic, R.string.analysis_legal_references_commercial_law_description, R.string.analysis_legal_references_commercial_law_effective_date, "luật thương mại 2005 điều 301 mức phạt vi phạm"),
        AnalysisLegalReference("civil-code-2015-article-418", R.string.analysis_legal_references_civil_code_title, R.string.analysis_legal_references_country_vietnam, R.string.analysis_legal_references_civil_code_article, R.string.analysis_legal_references_civil_code_topic, R.string.analysis_legal_references_civil_code_description, R.string.analysis_legal_references_civil_code_effective_date, "bộ luật dân sự 2015 điều 418 thỏa thuận phạt vi phạm"),
        AnalysisLegalReference("decree-21-2021-article-15", R.string.analysis_legal_references_decree_title, R.string.analysis_legal_references_country_vietnam, R.string.analysis_legal_references_decree_article, R.string.analysis_legal_references_decree_topic, R.string.analysis_legal_references_decree_description, R.string.analysis_legal_references_decree_effective_date, "nghị định 21 2021 điều 15 bảo đảm thực hiện nghĩa vụ")
    )

    fun canView(id: String, unlocked: Boolean): Boolean =
        (if (unlocked) items else items.take(com.example.lagallens.domain.model.analysis.AnalysisPreview.visibleCount(items.size, false))).any { it.id == id }
}
