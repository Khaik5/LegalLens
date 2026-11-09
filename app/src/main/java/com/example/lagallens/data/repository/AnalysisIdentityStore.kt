package com.example.lagallens.data.repository

import com.example.lagallens.domain.model.analysis.AnalysisIdentity
import java.util.UUID

/** The caller serializes each operation with session changes and account data mutations. */
internal class AnalysisIdentityStore(
    private val read: () -> List<AnalysisIdentity>,
    private val write: (List<AnalysisIdentity>) -> Unit,
    private val linkedDocument: (String, String) -> Boolean,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    private val snapshot: () -> Map<String, String>? = { null },
    private val ownerId: String? = null
) {
    private fun belongsToOwner(record: AnalysisIdentity): Boolean {
        val expected = ownerId ?: return true
        if (record.snapshot == null) return true // The caller must treat missing content as unavailable.
        return AnalysisSnapshotPayloadValidator.isValid(record, expected)
    }

    fun create(operationId: String, contractId: String, documentId: String): AnalysisIdentity {
        require(operationId.isNotBlank() && contractId.isNotBlank() && documentId.isNotBlank())
        check(linkedDocument(contractId, documentId)) { "Contract or document unavailable" }
        val records = read()
        records.firstOrNull { it.operationId == operationId }?.let {
            require(it.contractId == contractId && it.documentId == documentId && belongsToOwner(it)) { "Conflicting analysis operation" }
            check(ownerId == null || it.snapshot != null) { "Analysis content unavailable" }
            return it
        }
        val id = newId()
        check(id.isNotBlank() && records.none { it.resultId == id }) { "Duplicate analysis result ID" }
        val captured = snapshot()?.toMutableMap()?.apply {
            put("identity:resultId", id)
            put("identity:contractId", contractId)
            put("identity:documentId", documentId)
            ownerId?.let { put("identity:accountId", it) }
        }
        val factsCount = captured?.get("layout:facts")?.let { org.json.JSONArray(it).length() } ?: 0
        val result = AnalysisIdentity(id, operationId, contractId, documentId, listOf("penalty-cap", "termination-notice", "dispute-resolution"),
            com.example.lagallens.domain.model.analysis.AnalysisOverview(65, factsCount, 3, 0), captured)
        if (ownerId != null) check(AnalysisSnapshotPayloadValidator.isValid(result, ownerId)) { "Invalid analysis snapshot" }
        if (ownerId != null) check(AnalysisSnapshotPayloadValidator.hasConsistentOverview(result)) { "Inconsistent analysis overview" }
        write(records + result)
        return result
    }

    fun get(resultId: String): AnalysisIdentity? {
        if (resultId.isBlank()) return null
        val result = read().singleOrNull { it.resultId == resultId } ?: return null
        return result.takeIf { belongsToOwner(it) && linkedDocument(it.contractId, it.documentId) }
    }

    fun latest(contractId: String): AnalysisIdentity? {
        val result = read().lastOrNull { it.contractId == contractId } ?: return null
        if (result.snapshot == null || !belongsToOwner(result) || !linkedDocument(result.contractId, result.documentId))
            throw com.example.lagallens.domain.repository.AnalysisContentUnavailableException()
        return result
    }
}
