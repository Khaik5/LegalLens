package com.example.lagallens.data.mapper

import com.example.lagallens.domain.model.analysis.AnalysisDetail
import com.example.lagallens.domain.model.analysis.AnalysisFinding
import com.example.lagallens.domain.model.analysis.AnalysisIdentity
import com.example.lagallens.domain.model.analysis.AnalysisKeyFactEntry
import com.example.lagallens.domain.model.analysis.AnalysisObligationEntry
import com.example.lagallens.domain.model.analysis.AnalysisPartyEntry
import com.example.lagallens.domain.model.analysis.AnalysisPartyFieldEntry
import com.example.lagallens.domain.model.analysis.AnalysisSeverity
import com.example.lagallens.domain.model.analysis.AnalysisSourceExcerpt
import com.example.lagallens.domain.model.analysis.AnalysisSummaryEntry
import org.json.JSONArray
import org.json.JSONObject

/** Missing provenance means that older English values cannot be trusted as translations. */
internal fun snapshotTextForLanguage(snapshot: Map<String, String>, name: String, language: String): String? {
    if (language == "en" && snapshot["translation:en"] != "complete-v1") return null
    return snapshot["$language:$name"]?.takeIf { it.isNotBlank() }
}

/** Reads the stored DEMO snapshot into the domain model. A backend mapper replaces this file. */
internal class DemoAnalysisSnapshotMapper(private val language: String, private val unavailableText: String) {
    fun toDetail(identity: AnalysisIdentity): AnalysisDetail {
        val s = requireNotNull(identity.snapshot) { "Analysis content unavailable" }
        require(s["identity:resultId"] == identity.resultId && s["identity:contractId"] == identity.contractId && s["identity:documentId"] == identity.documentId)
        require(!s["identity:accountId"].isNullOrBlank())
        val version = (s["schema:version"] ?: "1").toInt().also { require(it in 1..3) }
        distinct(identity.findingIds)
        return AnalysisDetail(
            summary(s, version), facts(s, version), parties(s, version), obligations(s, version),
            identity.findingIds.map { finding(s, it) }
        )
    }

    private fun text(s: Map<String, String>, name: String): String {
        require(name.isNotBlank() && !s["vi:$name"].isNullOrBlank() && !s["en:$name"].isNullOrBlank())
        return snapshotTextForLanguage(s, name, language) ?: unavailableText
    }

    private fun rows(s: Map<String, String>, key: String): List<List<String>> {
        val outer = JSONArray(s.getValue(key))
        return (0 until outer.length()).map { i ->
            val row = outer.getJSONArray(i)
            (0 until row.length()).map { row.get(it) as? String ?: error("Invalid snapshot layout field") }
        }
    }

    private fun distinct(ids: List<String>) = require(ids.all { it.isNotBlank() } && ids.distinct().size == ids.size)

    private fun summary(s: Map<String, String>, v: Int) = rows(s, "layout:summary").map { r ->
        require(r.size == if (v >= 2) 3 else 2)
        val i = if (v >= 2) 1 else 0
        AnalysisSummaryEntry(r[0], text(s, r[i]), text(s, r[i + 1]))
    }.also { distinct(it.map { row -> row.id }) }

    private fun facts(s: Map<String, String>, v: Int) = rows(s, "layout:facts").map { r ->
        val i = if (v >= 2) 1 else 0
        require(r.size - i == 4 || r.size - i == 7)
        AnalysisKeyFactEntry(
            r[0], text(s, r[i]), text(s, r[i + 1]), text(s, r[i + 2]), text(s, r[i + 3]),
            if (r.size - i == 7) AnalysisSourceExcerpt(text(s, r[i + 4]), text(s, r[i + 5]), text(s, r[i + 6])) else null
        )
    }.also { distinct(it.map { row -> row.id }) }

    private fun parties(s: Map<String, String>, v: Int): List<AnalysisPartyEntry> {
        val array = JSONArray(s.getValue("layout:parties"))
        return (0 until array.length()).map { index ->
            val row = array.getJSONArray(index)
            if (v >= 3) {
                require(row.length() == 4)
                val fields = row.getJSONArray(3)
                val entries = (0 until fields.length()).map { fieldIndex ->
                    val field = fields.getJSONArray(fieldIndex)
                    require(field.length() == 3)
                    AnalysisPartyFieldEntry(field.getString(0), text(s, field.getString(1)), text(s, field.getString(2)))
                }
                distinct(entries.map { it.id })
                AnalysisPartyEntry(row.getString(0), text(s, row.getString(1)), text(s, row.getString(2)), entries)
            } else {
                val values = (0 until row.length()).map { row.get(it) as? String ?: error("Invalid party field") }
                val offset = if (v >= 2) 1 else 0
                require(values.size - offset >= 2 && (values.size - offset - 2) % 2 == 0)
                val fields = values.drop(offset + 2).chunked(2).map { pair -> AnalysisPartyFieldEntry(pair[0], text(s, pair[0]), text(s, pair[1])) }
                distinct(fields.map { it.id })
                AnalysisPartyEntry(values[0], text(s, values[offset]), text(s, values[offset + 1]), fields)
            }
        }.also { distinct(it.map { party -> party.id }) }
    }

    private fun obligations(s: Map<String, String>, v: Int): List<AnalysisObligationEntry> {
        var sectionIndex = 0
        val result = rows(s, "layout:obligations").map { r ->
            val i = if (v >= 2) 2 else 1
            val itemId = r.getOrNull(1).orEmpty()
            when (r.firstOrNull()) {
                "section" -> {
                    require(r.size - i == 1 || r.size - i == 2)
                    val tab = if (r.size - i == 2) com.example.lagallens.domain.model.analysis.AnalysisObligationTab.valueOf(r[i + 1].uppercase())
                        else if (sectionIndex == 0) com.example.lagallens.domain.model.analysis.AnalysisObligationTab.RIGHTS else com.example.lagallens.domain.model.analysis.AnalysisObligationTab.DUTIES
                    sectionIndex++
                    AnalysisObligationEntry.Section(itemId, text(s, r[i]), tab)
                }
                "obligation" -> {
                    require(r.size - i == 3 || r.size - i == 6)
                    val source = if (r.size - i == 6) AnalysisSourceExcerpt(text(s, r[i + 3]), text(s, r[i + 4]), text(s, r[i + 5])) else null
                    AnalysisObligationEntry.Item(itemId, text(s, r[i]), text(s, r[i + 1]), text(s, r[i + 2]), source)
                }
                else -> error("Unknown obligation type")
            }
        }
        distinct(result.map { entry -> when (entry) { is AnalysisObligationEntry.Section -> entry.id; is AnalysisObligationEntry.Item -> entry.id } })
        return result
    }

    private fun finding(s: Map<String, String>, findingId: String): AnalysisFinding {
        val row = JSONObject(s.getValue("layout:findings")).getJSONArray(findingId)
        require(row.length() == 4 && findingId.isNotBlank())
        val keys = (0 until row.length()).map { row.get(it) as? String ?: error("Invalid finding layout field") }
        return AnalysisFinding(
            findingId, AnalysisSeverity.valueOf(s.getValue("risk:$findingId:severity")),
            text(s, keys[0]), text(s, keys[1]), text(s, keys[2]), text(s, keys[3]),
            s["risk:$findingId:explanation"]?.let { text(s, it) },
            s["risk:$findingId:recommendation"]?.let { text(s, it) }
        )
    }
}
