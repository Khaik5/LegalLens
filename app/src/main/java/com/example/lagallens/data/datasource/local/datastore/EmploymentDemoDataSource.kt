package com.example.lagallens.data.datasource.local.datastore

import android.content.Context
import com.example.lagallens.R
import com.example.lagallens.domain.model.EmploymentContract
import com.example.lagallens.domain.model.EmploymentContractRisk
import com.example.lagallens.domain.model.EmploymentContractStatus
import com.example.lagallens.domain.model.EmploymentDocument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Local, persistent DEMO store. Contract text is generic and contains no real people or organizations. */
class EmploymentDemoDataSource internal constructor(private val preferences: android.content.SharedPreferences, private val title: (Int) -> String) {
    constructor(context: Context, preferencesName: String = PREFERENCES_NAME) : this(
        context.applicationContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE),
        { context.applicationContext.getString(it) }
    )
    private val documents = loadDocuments().toMutableMap()
    private val contractsFlow = MutableStateFlow(loadContracts())

    val contracts = contractsFlow.asStateFlow()

    @Synchronized
    fun getContract(contractId: String): EmploymentContract? = contractsFlow.value.firstOrNull { it.id == contractId }

    @Synchronized
    fun createContract(contract: EmploymentContract): EmploymentContract {
        val stored = contract.copy(
            id = contract.id.ifBlank { UUID.randomUUID().toString() },
            documentId = contract.documentId.ifBlank { UUID.randomUUID().toString() },
            title = contract.title.trim().ifBlank { "Hợp đồng lao động mới" },
            updatedAtMillis = System.currentTimeMillis()
        )
        require(getContract(stored.id) == null) { "Duplicate contractId" }
        require(documents[stored.documentId]?.contractId.let { it == null || it == stored.id }) { "Document belongs to another contract" }
        persistContracts(contractsFlow.value + stored)
        val existingDocument = documents[stored.documentId]
        saveDocument(EmploymentDocument(stored.documentId, stored.id, existingDocument?.displayName ?: stored.fileName.ifBlank { stored.title }, stored.fileUri ?: existingDocument?.uri, stored.mimeType.ifBlank { existingDocument?.mimeType.orEmpty() }, stored.content, stored.updatedAtMillis))
        return stored
    }

    @Synchronized
    fun updateContract(contract: EmploymentContract): Boolean {
        val existing = getContract(contract.id) ?: return false
        require(existing.documentId == contract.documentId) { "Cannot replace linked documentId" }
        val updated = contract.copy(title = contract.title.trim(), updatedAtMillis = System.currentTimeMillis())
        persistContracts(contractsFlow.value.map { if (it.id == contract.id) updated else it })
        val oldDocument = documents[updated.documentId]
        saveDocument(EmploymentDocument(updated.documentId, updated.id, oldDocument?.displayName ?: updated.fileName.ifBlank { updated.title }, updated.fileUri ?: oldDocument?.uri, updated.mimeType.ifBlank { oldDocument?.mimeType.orEmpty() }, updated.content, updated.updatedAtMillis))
        return true
    }

    @Synchronized
    fun renameContract(contractId: String, title: String): Boolean {
        if (title.isBlank()) return false
        val current = getContract(contractId) ?: return false
        return updateContract(current.copy(title = title))
    }

    @Synchronized
    fun deleteContract(contractId: String): Boolean {
        val current = getContract(contractId) ?: return false
        persistContracts(contractsFlow.value.filterNot { it.id == contractId })
        documents.remove(current.documentId)
        persistDocuments()
        return true
    }

    @Synchronized
    fun getDocument(documentId: String): EmploymentDocument? = documents[documentId]

    @Synchronized
    fun saveDocument(document: EmploymentDocument): EmploymentDocument {
        document.contractId?.let { id ->
            val contract = requireNotNull(getContract(id)) { "Unknown contractId in current account" }
            require(contract.documentId == document.id) { "Document/contract ID mismatch" }
        }
        val saved = document.copy(updatedAtMillis = System.currentTimeMillis())
        documents[saved.id] = saved
        persistDocuments()
        if (!saved.contractId.isNullOrBlank()) {
            getContract(saved.contractId)?.let { contract ->
                persistContracts(contractsFlow.value.map { if (it.id == contract.id) contract.copy(content = saved.content, updatedAtMillis = saved.updatedAtMillis) else it })
            }
        }
        return saved
    }

    @Synchronized
    fun updateDocumentContent(documentId: String, contractId: String?, content: String): Boolean {
        val current = documents[documentId] ?: return false
        saveDocument(current.copy(contractId = contractId ?: current.contractId, content = content))
        return true
    }

    @Synchronized
    fun deleteDocument(documentId: String): Boolean {
        if (documents.remove(documentId) == null) return false
        persistDocuments()
        return true
    }

    @Synchronized
    fun deleteContractForDocument(documentId: String): Boolean {
        val contract = contractsFlow.value.firstOrNull { it.documentId == documentId }
        return if (contract == null) deleteDocument(documentId) else deleteContract(contract.id)
    }

    private fun loadContracts(): List<EmploymentContract> {
        val encoded = preferences.getString(CONTRACTS_KEY, null)
        if (encoded != null) return decodeContracts(encoded)
        val now = System.currentTimeMillis()
        val legacyDeleted = preferences.getStringSet(LEGACY_DELETED_IDS_KEY, emptySet()).orEmpty()
        val seeds = sampleContracts(now).filterNot { it.id in legacyDeleted }.map { sample ->
            sample.copy(title = preferences.getString(LEGACY_RENAMED_TITLE_PREFIX + sample.id, null) ?: sample.title)
        }
        check(preferences.edit().putString(CONTRACTS_KEY, encodeContracts(seeds)).commit())
        if (preferences.getString(DOCUMENTS_KEY, null) == null) {
            seeds.forEach { contract -> documents[contract.documentId] = contract.toDocument() }
            persistDocuments()
        }
        return seeds
    }

    private fun sampleContracts(now: Long): List<EmploymentContract> {
        val samples = listOf(
            Sample("office-lease", R.string.contract_demo_labor_indefinite, "Bản DEMO · Hợp đồng lao động không xác định thời hạn", "ĐIỀU 1. CÔNG VIỆC VÀ ĐỊA ĐIỂM\n[Chức danh, nhiệm vụ và nơi làm việc]\n\nĐIỀU 2. THỜI HẠN\n[Không xác định thời hạn]\n\nĐIỀU 3. TIỀN LƯƠNG\n[Mức lương, phụ cấp và kỳ hạn trả lương]\n\nĐIỀU 4. THỜI GIỜ LÀM VIỆC VÀ NGHỈ NGƠI\n[Thời giờ làm việc, nghỉ hằng tuần và nghỉ phép]\n\nĐIỀU 5. QUYỀN VÀ NGHĨA VỤ\n[Quyền và nghĩa vụ của các bên]", EmploymentContractStatus.COMPLETE, EmploymentContractRisk.LOW),
            Sample("collaborator", R.string.contract_demo_labor_fixed_term, "Bản DEMO · Hợp đồng lao động xác định thời hạn", "ĐIỀU 1. CÔNG VIỆC VÀ ĐỊA ĐIỂM\n[Chức danh, nhiệm vụ và nơi làm việc]\n\nĐIỀU 2. THỜI HẠN\n[Thời hạn hợp đồng, ngày bắt đầu và ngày kết thúc]\n\nĐIỀU 3. TIỀN LƯƠNG\n[Mức lương, phụ cấp và kỳ hạn trả lương]\n\nĐIỀU 4. THỜI GIỜ LÀM VIỆC VÀ NGHỈ NGƠI\n[Thời giờ làm việc, nghỉ hằng tuần và nghỉ phép]\n\nĐIỀU 5. CHẤM DỨT HỢP ĐỒNG\n[Điều kiện và trách nhiệm khi chấm dứt]", EmploymentContractStatus.PROCESSING, EmploymentContractRisk.HIGH),
            Sample("equipment-purchase", R.string.contract_demo_labor_probation, "Bản DEMO · Thỏa thuận thử việc", "ĐIỀU 1. CÔNG VIỆC THỬ VIỆC\n[Chức danh, nhiệm vụ và nơi làm việc]\n\nĐIỀU 2. THỜI GIAN THỬ VIỆC\n[Thời gian bắt đầu và kết thúc thử việc]\n\nĐIỀU 3. TIỀN LƯƠNG THỬ VIỆC\n[Mức lương và kỳ hạn trả lương]\n\nĐIỀU 4. KẾT QUẢ THỬ VIỆC\n[Thông báo kết quả và bước tiếp theo]", EmploymentContractStatus.DRAFT, EmploymentContractRisk.NOT_ANALYZED),
            Sample("vehicle-lease", R.string.contract_demo_labor_part_time, "Bản DEMO · Hợp đồng lao động bán thời gian", "ĐIỀU 1. CÔNG VIỆC VÀ ĐỊA ĐIỂM\n[Chức danh, nhiệm vụ và nơi làm việc]\n\nĐIỀU 2. THỜI GIAN LÀM VIỆC\n[Số giờ và lịch làm việc]\n\nĐIỀU 3. TIỀN LƯƠNG\n[Mức lương, phụ cấp và kỳ hạn trả lương]\n\nĐIỀU 4. QUYỀN VÀ NGHĨA VỤ\n[Quyền và nghĩa vụ của các bên]", EmploymentContractStatus.COMPLETE, EmploymentContractRisk.LOW)
        )
        return samples.mapIndexed { index, sample ->
            val contractId = sample.id
            EmploymentContract(contractId, "document-$contractId", title(sample.titleRes), sample.content, now - (index + 1) * 86_400_000L, now - index * 86_400_000L, sample.status, sample.risk, "${title(sample.titleRes)}.pdf", null, "application/pdf")
        }
    }

    private fun persistContracts(value: List<EmploymentContract>) {
        check(preferences.edit().putString(CONTRACTS_KEY, encodeContracts(value)).commit())
        contractsFlow.value = value
    }

    private fun persistDocuments() {
        check(preferences.edit().putString(DOCUMENTS_KEY, JSONArray().apply { documents.values.forEach { put(it.toJson()) } }.toString()).commit())
    }

    private fun loadDocuments(): Map<String, EmploymentDocument> {
        val encoded = preferences.getString(DOCUMENTS_KEY, null) ?: return emptyMap()
        val array = JSONArray(encoded)
        return buildMap { repeat(array.length()) { index -> array.optJSONObject(index)?.toDocument()?.let { put(it.id, it) } } }
    }

    private fun encodeContracts(value: List<EmploymentContract>) = JSONArray().apply { value.forEach { put(it.toJson()) } }.toString()
    private fun decodeContracts(encoded: String): List<EmploymentContract> {
        val array = JSONArray(encoded)
        return buildList { repeat(array.length()) { index -> array.optJSONObject(index)?.toContract()?.let(::add) } }
    }

    private fun EmploymentContract.toJson() = JSONObject().put("id", id).put("documentId", documentId).put("title", title).put("content", content)
        .put("createdAtMillis", createdAtMillis).put("updatedAtMillis", updatedAtMillis).put("status", status.name).put("risk", risk.name)
        .put("fileName", fileName).put("fileUri", fileUri).put("mimeType", mimeType)

    private fun JSONObject.toContract() = EmploymentContract(
        id = optString("id"), documentId = optString("documentId"), title = optString("title"), content = optString("content"),
        createdAtMillis = optLong("createdAtMillis"), updatedAtMillis = optLong("updatedAtMillis"),
        status = runCatching { EmploymentContractStatus.valueOf(optString("status")) }.getOrDefault(EmploymentContractStatus.DRAFT),
        risk = runCatching { EmploymentContractRisk.valueOf(optString("risk")) }.getOrDefault(EmploymentContractRisk.NOT_ANALYZED),
        fileName = optString("fileName"), fileUri = optString("fileUri").takeIf(String::isNotBlank), mimeType = optString("mimeType")
    )

    private fun EmploymentDocument.toJson() = JSONObject().put("id", id).put("contractId", contractId).put("displayName", displayName).put("uri", uri)
        .put("mimeType", mimeType).put("content", content).put("updatedAtMillis", updatedAtMillis)

    private fun JSONObject.toDocument() = EmploymentDocument(
        id = optString("id"), contractId = optString("contractId").takeIf(String::isNotBlank), displayName = optString("displayName"),
        uri = optString("uri").takeIf(String::isNotBlank), mimeType = optString("mimeType"), content = optString("content"), updatedAtMillis = optLong("updatedAtMillis")
    )

    private fun EmploymentContract.toDocument() = EmploymentDocument(documentId, id, fileName.ifBlank { title }, fileUri, mimeType, content, updatedAtMillis)

    private data class Sample(val id: String, val titleRes: Int, val subtitle: String, val content: String, val status: EmploymentContractStatus, val risk: EmploymentContractRisk)

    private companion object {
        const val PREFERENCES_NAME = "contract_demo_data"
        const val CONTRACTS_KEY = "employment_contracts_v2"
        const val DOCUMENTS_KEY = "employment_documents_v1"
        const val LEGACY_DELETED_IDS_KEY = "deleted_contract_ids"
        const val LEGACY_RENAMED_TITLE_PREFIX = "renamed_title_"
    }
}
