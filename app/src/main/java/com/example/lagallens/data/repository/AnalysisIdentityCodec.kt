package com.example.lagallens.data.repository

import com.example.lagallens.domain.model.analysis.AnalysisIdentity
import com.example.lagallens.domain.model.analysis.AnalysisOverview
import org.json.JSONArray
import org.json.JSONObject

internal object AnalysisIdentityCodec {
    private fun JSONObject.requiredString(name: String): String = get(name) as? String ?: error("Invalid analysis field: $name")

    fun decode(raw: String): List<AnalysisIdentity> {
        val array = JSONArray(raw)
        return (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            AnalysisIdentity(item.requiredString("resultId"), item.requiredString("operationId"),
                item.requiredString("contractId"), item.requiredString("documentId"),
                if (!item.has("findingIds") && item.isNull("snapshot")) emptyList() else
                    item.getJSONArray("findingIds").let { ids -> (0 until ids.length()).map { ids.get(it) as? String ?: error("Invalid finding ID") } },
                item.optJSONObject("overview")?.let { overview -> AnalysisOverview(overview.getInt("score"), overview.getInt("keyFactsCount"), overview.getInt("riskCount"), overview.getInt("referenceCount")) },
                item.optJSONObject("snapshot")?.let { snapshot -> snapshot.keys().asSequence().associateWith { snapshot.get(it) as? String ?: error("Invalid snapshot field") } })
        }.also { records ->
            check(records.all { it.resultId.isNotBlank() && it.operationId.isNotBlank() && it.contractId.isNotBlank() && it.documentId.isNotBlank() })
            check(records.map { it.resultId }.distinct().size == records.size)
            check(records.map { it.operationId }.distinct().size == records.size)
        }
    }

    fun encode(records: List<AnalysisIdentity>): String = JSONArray().apply {
        records.forEach { item -> put(JSONObject().put("resultId", item.resultId).put("operationId", item.operationId)
            .put("contractId", item.contractId).put("documentId", item.documentId)
            .put("findingIds", JSONArray(item.findingIds))
            .put("overview", item.overview?.let { overview -> JSONObject().put("score", overview.score)
                .put("keyFactsCount", overview.keyFactsCount).put("riskCount", overview.riskCount).put("referenceCount", overview.referenceCount) })
            .put("snapshot", item.snapshot?.let { JSONObject(it) })) }
    }.toString()
}
