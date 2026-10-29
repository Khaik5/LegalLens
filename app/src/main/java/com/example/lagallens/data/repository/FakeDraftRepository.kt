package com.example.lagallens.data.repository

import android.content.Context
import com.example.lagallens.domain.model.Draft
import com.example.lagallens.domain.model.DraftStage
import com.example.lagallens.domain.repository.DraftRepository
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class FakeDraftRepository(
    context: Context,
    preferencesName: String = PREFERENCES_NAME,
    legacyPreferencesName: String = LEGACY_PREFERENCES_NAME
) : DraftRepository {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    private val legacyPreferences = appContext.getSharedPreferences(legacyPreferencesName, Context.MODE_PRIVATE)
    private val mutex = Mutex()

    override suspend fun getDrafts(): List<Draft> = mutex.withLock {
        migrateLegacyDrafts()
        readDrafts().sortedWith(compareBy<Draft> { it.createdAt }.thenBy { it.versionNo })
    }

    override suspend fun getDraft(draftId: String): Draft? = mutex.withLock {
        migrateLegacyDrafts()
        readDrafts().firstOrNull { it.draftId == draftId }
            ?: DraftStage.fromLegacyKey(draftId)?.let { getLatestDraftInternal(it) }
    }

    override suspend fun getLatestDraft(stage: DraftStage): Draft? = mutex.withLock {
        migrateLegacyDrafts()
        getLatestDraftInternal(stage)
    }

    override suspend fun saveDraft(
        stage: DraftStage,
        title: String,
        content: String,
        contractId: String?,
        sourceDraftId: String?
    ): Draft = mutex.withLock {
        migrateLegacyDrafts()
        val source = sourceDraftId?.let { findDraft(it) }
        val latestStageDraft = getLatestDraftInternal(stage)
        val latest = latestStageDraft ?: source
        val normalizedTitle = title.trim().ifBlank { DEFAULT_TITLE }
        val resolvedContractId = contractId ?: source?.contractId ?: latestStageDraft?.contractId ?: latest?.contractId
        if (latestStageDraft != null && latestStageDraft.title == normalizedTitle && latestStageDraft.content == content && latestStageDraft.contractId == resolvedContractId) {
            // Reconcile the Phase 1 compatibility mirror even when the canonical record is unchanged.
            persist(latestStageDraft, mirrorLegacy = true)
            return@withLock latestStageDraft
        }
        val records = readDrafts()
        val lineageId = source?.lineageId ?: latest?.lineageId ?: UUID.randomUUID().toString()
        val versionNo = (records.filter { it.lineageId == lineageId }.maxOfOrNull { it.versionNo } ?: 0) + 1
        val now = System.currentTimeMillis()
        val draft = Draft(
            draftId = UUID.randomUUID().toString(),
            contractId = resolvedContractId,
            title = normalizedTitle,
            content = content,
            stage = stage,
            versionNo = versionNo,
            createdAt = now,
            updatedAt = now,
            parentDraftId = source?.draftId ?: latest?.draftId,
            lineageId = lineageId
        )
        persist(draft, mirrorLegacy = true)
        draft
    }

    override suspend fun createCopy(sourceDraftId: String, title: String, stage: DraftStage): Draft? = mutex.withLock {
        migrateLegacyDrafts()
        val source = findDraft(sourceDraftId) ?: return@withLock null
        val now = System.currentTimeMillis()
        val versionNo = (readDrafts().filter { it.lineageId == source.lineageId }.maxOfOrNull { it.versionNo } ?: source.versionNo) + 1
        val copy = source.copy(
            draftId = UUID.randomUUID().toString(),
            title = title.trim().ifBlank { source.title },
            stage = stage,
            versionNo = versionNo,
            createdAt = now,
            updatedAt = now,
            parentDraftId = source.draftId
        )
        persist(copy, mirrorLegacy = true)
        copy
    }

    override suspend fun updateDraft(draftId: String, title: String, content: String): Draft? = mutex.withLock {
        migrateLegacyDrafts()
        val existing = findDraft(draftId) ?: return@withLock null
        val now = System.currentTimeMillis()
        val versionNo = (readDrafts().filter { it.lineageId == existing.lineageId }.maxOfOrNull { it.versionNo } ?: existing.versionNo) + 1
        val updatedVersion = existing.copy(
            draftId = UUID.randomUUID().toString(),
            title = title.trim().ifBlank { existing.title },
            content = content,
            versionNo = versionNo,
            createdAt = now,
            updatedAt = now,
            parentDraftId = existing.draftId
        )
        persist(updatedVersion, mirrorLegacy = true)
        updatedVersion
    }

    override suspend fun deleteDraft(draftId: String): Boolean = mutex.withLock {
        migrateLegacyDrafts()
        val draft = findDraft(draftId) ?: return@withLock false
        val ids = readIds().filterNot { it == draft.draftId }
        val editor = preferences.edit().remove(recordKey(draft.draftId)).putString(KEY_IDS, JSONArray(ids).toString())
        val remainingStageDraft = draftsForStage(draft.stage).filterNot { it.draftId == draft.draftId }
            .maxWithOrNull(compareBy<Draft> { it.versionNo }.thenBy { it.updatedAt })
        if (remainingStageDraft == null) {
            legacyPreferences.edit().remove(legacyNameKey(draft.stage.legacyKey)).remove(legacyContentKey(draft.stage.legacyKey)).remove(legacyUpdatedKey(draft.stage.legacyKey)).apply()
        } else {
            legacyPreferences.edit()
                .putString(legacyNameKey(draft.stage.legacyKey), remainingStageDraft.title)
                .putString(legacyContentKey(draft.stage.legacyKey), remainingStageDraft.content)
                .putString(legacyUpdatedKey(draft.stage.legacyKey), remainingStageDraft.updatedAt.toString())
                .apply()
        }
        editor.commit()
    }

    private fun migrateLegacyDrafts() {
        if (preferences.getBoolean(KEY_MIGRATION_COMPLETE, false)) return
        val now = System.currentTimeMillis()
        val additions = DraftStage.entries.mapNotNull { stage ->
            val oldName = legacyPreferences.getString(legacyNameKey(stage.legacyKey), null) ?: return@mapNotNull null
            val legacyId = legacyId(stage)
            if (findDraft(legacyId) != null) return@mapNotNull null
            val legacyUpdatedAt = legacyPreferences.getString(legacyUpdatedKey(stage.legacyKey), null)?.toLongOrNull() ?: now
            Draft(
                draftId = legacyId,
                contractId = null,
                title = oldName,
                content = legacyPreferences.getString(legacyContentKey(stage.legacyKey), null).orEmpty(),
                stage = stage,
                versionNo = 1,
                createdAt = legacyUpdatedAt,
                updatedAt = legacyUpdatedAt,
                lineageId = "legacy-${stage.legacyKey}"
            )
        }
        val editor = preferences.edit().putBoolean(KEY_MIGRATION_COMPLETE, true)
        val ids = readIds().toMutableList()
        additions.forEach { draft ->
            editor.putString(recordKey(draft.draftId), draft.toJson().toString())
            ids += draft.draftId
        }
        check(editor.putString(KEY_IDS, JSONArray(ids.distinct()).toString()).commit())
    }

    private fun getLatestDraftInternal(stage: DraftStage): Draft? = draftsForStage(stage)
        .maxWithOrNull(compareBy<Draft> { it.versionNo }.thenBy { it.updatedAt })

    private fun draftsForStage(stage: DraftStage): List<Draft> = readDrafts().filter { it.stage == stage }

    private fun findDraft(idOrLegacyKey: String): Draft? = readDrafts().firstOrNull { it.draftId == idOrLegacyKey }
        ?: DraftStage.fromLegacyKey(idOrLegacyKey)?.let { readDrafts().firstOrNull { draft -> draft.draftId == legacyId(it) } }

    private fun persist(draft: Draft, mirrorLegacy: Boolean) {
        val ids = readIds().toMutableSet().apply { add(draft.draftId) }
        val saved = preferences.edit()
            .putString(recordKey(draft.draftId), draft.toJson().toString())
            .putString(KEY_IDS, JSONArray(ids.toList()).toString())
            .commit()
        check(saved)
        if (mirrorLegacy) {
            val alias = draft.stage.legacyKey
            check(legacyPreferences.edit()
                .putString(legacyNameKey(alias), draft.title)
                .putString(legacyContentKey(alias), draft.content)
                .putString(legacyUpdatedKey(alias), draft.updatedAt.toString())
                .commit())
        }
    }

    private fun readDrafts(): List<Draft> = readIds().mapNotNull { id ->
        preferences.getString(recordKey(id), null)?.let { raw -> runCatching { JSONObject(raw).toDraft() }.getOrNull() }
    }

    private fun readIds(): List<String> = runCatching {
        val array = JSONArray(preferences.getString(KEY_IDS, "[]"))
        List(array.length()) { index -> array.getString(index) }
    }.getOrDefault(emptyList())

    private fun Draft.toJson() = JSONObject().apply {
        put("draftId", draftId)
        put("contractId", contractId)
        put("title", title)
        put("content", content)
        put("stage", stage.name)
        put("versionNo", versionNo)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
        put("parentDraftId", parentDraftId)
        put("lineageId", lineageId)
    }

    private fun JSONObject.toDraft() = Draft(
        draftId = getString("draftId"),
        contractId = optString("contractId").takeIf(String::isNotBlank),
        title = optString("title"),
        content = optString("content"),
        stage = DraftStage.valueOf(getString("stage")),
        versionNo = getInt("versionNo"),
        createdAt = getLong("createdAt"),
        updatedAt = getLong("updatedAt"),
        parentDraftId = optString("parentDraftId").takeIf(String::isNotBlank),
        lineageId = optString("lineageId").ifBlank { getString("draftId") }
    )

    private fun recordKey(id: String) = "draft_record_$id"

    private fun legacyId(stage: DraftStage) = "legacy-${stage.legacyKey}"

    private fun legacyNameKey(key: String) = "draft_${key.lowercase().replace(Regex("[^a-z0-9]+"), "_")}"

    private fun legacyContentKey(key: String) = "${legacyNameKey(key)}_content"

    private fun legacyUpdatedKey(key: String) = "${legacyNameKey(key)}_updated_at"

    private companion object {
        const val PREFERENCES_NAME = "draft_repository_v1"
        const val LEGACY_PREFERENCES_NAME = "saved_draft_copies"
        const val KEY_IDS = "draft_ids"
        const val KEY_MIGRATION_COMPLETE = "legacy_saved_drafts_migrated"
        const val DEFAULT_TITLE = "Hợp đồng lao động DEMO"
    }
}
