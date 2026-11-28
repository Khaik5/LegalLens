package com.example.lagallens.presentation.feature.compare.result.utils

private val articleHeading = Regex("""^\s*(Điều\s+\d+[a-zđ]?\s*[.:]?\s*[^:\n]*)""", RegexOption.IGNORE_CASE)

/**
 * A clause title is the first line of the clause, which usually runs on into its content
 * ("Điều 3. Thời giờ làm việc: 48 giờ/tuần..."). Only the article number and its name are shown.
 */
fun String.toClauseHeading(maxLength: Int = 60): String {
    val firstLine = trim().lineSequence().firstOrNull().orEmpty().trim()
    val heading = articleHeading.find(firstLine)?.groupValues?.get(1)?.trim()?.trimEnd('.', ' ')
        ?.takeIf { it.isNotBlank() } ?: firstLine
    return if (heading.length <= maxLength) heading else heading.take(maxLength).trimEnd() + "…"
}
