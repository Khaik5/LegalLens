package com.example.lagallens.presentation.feature.compare.result.utils

import com.example.lagallens.core.utils.foldForSearch
import com.example.lagallens.domain.model.ContractKeyFact

/** One row of the side-by-side key facts; a side is null when its analysis has no matching fact. */
data class KeyFactRow(val label: String, val first: String?, val second: String?)

/**
 * Each analysis names its key facts in its own words ("Lương cơ bản", "Mức lương chính"), so facts are paired by
 * topic first and by their exact label otherwise. Rows follow the first contract's order; facts only the second
 * contract has come last.
 */
internal fun pairKeyFacts(first: List<ContractKeyFact>, second: List<ContractKeyFact>): List<KeyFactRow> {
    val remaining = second.toMutableList()
    val rows = first.map { fact ->
        val key = fact.pairingKey()
        val match = remaining.firstOrNull { it.pairingKey() == key }?.also { remaining.remove(it) }
        KeyFactRow(fact.label, fact.value, match?.value)
    }
    return rows + remaining.map { KeyFactRow(it.label, null, it.value) }
}

private fun ContractKeyFact.pairingKey(): String {
    val folded = label.foldForSearch().trim()
    return TOPICS.firstOrNull { (_, words) -> words.any { folded.contains(it) } }?.first ?: "label:$folded"
}

/** Ordered: a label is given the first topic it mentions, so "phụ cấp lương" is an allowance, not the salary. */
private val TOPICS = listOf(
    "allowance" to listOf("phu cap", "allowance"),
    "bonus" to listOf("thuong", "bonus"),
    "salary" to listOf("luong", "salary", "wage"),
    "term" to listOf("thoi han", "loai hop dong", "term", "duration"),
    "probation" to listOf("thu viec", "probation"),
    "hours" to listOf("thoi gio", "gio lam", "working hours"),
    "leave" to listOf("nghi phep", "phep nam", "annual leave"),
    "place" to listOf("dia diem", "noi lam viec", "workplace", "location"),
    "insurance" to listOf("bao hiem", "insurance"),
    "position" to listOf("chuc danh", "vi tri", "cong viec", "position", "job")
)
