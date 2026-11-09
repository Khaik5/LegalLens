package com.example.lagallens.data.repository

import com.example.lagallens.domain.model.analysis.AnalysisIdentity
import org.json.JSONArray
import org.json.JSONObject

/** Checks stored result payload without loading presentation resources or changing legacy records. */
internal object AnalysisSnapshotPayloadValidator {
    fun hasConsistentOverview(identity: AnalysisIdentity): Boolean = runCatching {
        val s = requireNotNull(identity.snapshot)
        val overview = requireNotNull(identity.overview)
        require(overview.keyFactsCount == JSONArray(s.getValue("layout:facts")).length())
        require(overview.riskCount == identity.findingIds.size)
    }.isSuccess

    fun isValid(identity: AnalysisIdentity, owner: String): Boolean = runCatching {
        val s = requireNotNull(identity.snapshot)
        require(owner.isNotBlank() && s["identity:accountId"] == owner)
        require(s["identity:resultId"] == identity.resultId && s["identity:contractId"] == identity.contractId && s["identity:documentId"] == identity.documentId)
        val version = (s["schema:version"] ?: "1").toInt()
        require(version in 1..3)
        fun str(a: JSONArray, index: Int) = a.get(index) as? String ?: error("Invalid snapshot field")
        fun value(key: String) {
            require(key.isNotBlank() && !s["vi:$key"].isNullOrBlank() && !s["en:$key"].isNullOrBlank())
        }
        fun unique(values: List<String>) = require(values.all { it.isNotBlank() } && values.size == values.distinct().size)
        fun fixedRows(name: String, sizes: Set<Int>, typed: Boolean = false) {
            val outer = JSONArray(s.getValue(name))
            val ids = mutableListOf<String>()
            for (i in 0 until outer.length()) {
                val row = outer.getJSONArray(i)
                val offset = if (typed) { if (version >= 2) 2 else 1 } else { if (version >= 2) 1 else 0 }
                val count = row.length() - offset
                if (typed) {
                    val type = str(row, 0)
                    require((type == "section" && count in 1..2) || (type == "obligation" && count in setOf(3, 6)))
                    if (type == "section" && count == 2) require(str(row, offset + 1) in setOf("rights", "duties"))
                } else require(count in sizes)
                ids += if (version >= 2) str(row, if (typed) 1 else 0) else str(row, offset)
                for (j in offset until row.length()) {
                    if (typed && str(row, 0) == "section" && count == 2 && j == offset + 1) continue
                    value(str(row, j))
                }
            }
            unique(ids)
        }
        fixedRows("layout:summary", setOf(2))
        fixedRows("layout:facts", setOf(4, 7))
        fixedRows("layout:obligations", emptySet(), typed = true)
        val parties = JSONArray(s.getValue("layout:parties"))
        val partyIds = mutableListOf<String>()
        for (i in 0 until parties.length()) {
            val row = parties.getJSONArray(i)
            if (version >= 3) {
                require(row.length() == 4)
                partyIds += str(row, 0)
                value(str(row, 1)); value(str(row, 2))
                val fields = row.getJSONArray(3)
                val fieldIds = mutableListOf<String>()
                for (j in 0 until fields.length()) {
                    val field = fields.getJSONArray(j)
                    require(field.length() == 3)
                    fieldIds += str(field, 0); value(str(field, 1)); value(str(field, 2))
                }
                unique(fieldIds)
            } else {
                val offset = if (version >= 2) 1 else 0
                require(row.length() - offset >= 2 && (row.length() - offset - 2) % 2 == 0)
                partyIds += str(row, 0)
                for (j in offset until row.length()) value(str(row, j))
                unique((offset + 2 until row.length() step 2).map { str(row, it) })
            }
        }
        unique(partyIds)
        unique(identity.findingIds)
        val findings = JSONObject(s.getValue("layout:findings"))
        identity.findingIds.forEach { findingId ->
            val fields = findings.getJSONArray(findingId)
            require(fields.length() == 4 && s["risk:$findingId:severity"] in setOf("HIGH", "MEDIUM", "LOW"))
            for (i in 0 until fields.length()) value(str(fields, i))
            listOf("explanation", "recommendation").forEach { field ->
                s["risk:$findingId:$field"]?.let { value(it) }
            }
        }
    }.isSuccess
}
