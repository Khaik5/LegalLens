package com.example.lagallens.core.utils

import java.text.Normalizer

private val combiningMarks = Regex("\\p{Mn}+")

/**
 * Lower-case text without Vietnamese tone and vowel marks, so a query typed without diacritics, such as
 * "luong", still finds "lương". The letter đ has no decomposed form and is mapped to d explicitly.
 */
fun String.foldForSearch(): String =
    combiningMarks.replace(Normalizer.normalize(lowercase(), Normalizer.Form.NFD), "").replace('đ', 'd')

/** True when [query] (trimmed) is blank or occurs in this text, ignoring case and Vietnamese diacritics. */
fun String.matchesSearch(query: String): Boolean {
    val folded = query.trim().foldForSearch()
    return folded.isEmpty() || foldForSearch().contains(folded)
}
