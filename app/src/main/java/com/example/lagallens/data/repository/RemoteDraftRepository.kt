package com.example.lagallens.data.repository

import android.content.Context
import com.example.lagallens.core.network.ApiFailureMapper
import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.datasource.local.datastore.AuthSessionStore
import com.example.lagallens.data.datasource.local.datastore.currentSessionScope
import com.example.lagallens.data.datasource.remote.DraftRemoteDataSource
import com.example.lagallens.data.model.request.CreateDraftRequest
import com.example.lagallens.data.model.request.UpdateDraftRequest
import com.example.lagallens.data.model.response.DraftVersionResponse
import com.example.lagallens.domain.model.Draft
import com.example.lagallens.domain.model.DraftStage
import com.example.lagallens.domain.repository.DraftRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.time.Instant

/** The app-side meaning of one backend version; the backend only keeps text, so it cannot store this. */
data class DraftVersionMark(val stage: DraftStage, val isSaved: Boolean)

interface DraftVersionMarks {
    fun get(userId: String, draftId: Long, version: Int): DraftVersionMark?
    fun put(userId: String, draftId: Long, version: Int, mark: DraftVersionMark)
}

class SharedPreferencesDraftVersionMarks(context: Context) : DraftVersionMarks {
    private val preferences = context.applicationContext.getSharedPreferences("remote_draft_version_marks", Context.MODE_PRIVATE)

    override fun get(userId: String, draftId: Long, version: Int): DraftVersionMark? =
        preferences.getString(key(userId, draftId, version), null)?.split('|')?.takeIf { it.size == 2 }?.let { (stage, saved) ->
            DraftStage.fromLegacyKey(stage)?.let { DraftVersionMark(it, saved == "1") }
        }

    override fun put(userId: String, draftId: Long, version: Int, mark: DraftVersionMark) {
        preferences.edit().putString(key(userId, draftId, version), "${mark.stage.legacyKey}|${if (mark.isSaved) 1 else 0}").apply()
    }

    private fun key(userId: String, draftId: Long, version: Int) = "$userId:$draftId:$version"
}

/** The app's draft ID for one backend version, so D1, M1 and D2 of the same backend draft stay separate. */
internal object RemoteDraftId {
    fun of(draftId: Long, version: Int) = "$draftId:$version"

    fun parse(value: String): Pair<Long, Int>? {
        val parts = value.split(':')
        if (parts.size != 2) return null
        val draft = parts[0].toLongOrNull() ?: return null
        val version = parts[1].toIntOrNull() ?: return null
        return draft to version
    }
}

/**
 * Backend drafts as app drafts. Editing creates a new backend version of the same draft, so every earlier
 * version remains readable; a copy is a new backend draft. The list holds the versions of the newest drafts
 * without their text; [getDraft] loads the text.
 */
class RemoteDraftRepository(
    private val tokens: AuthSessionStore,
    private val remote: DraftRemoteDataSource,
    private val marks: DraftVersionMarks,
    private val listSize: Int = LIST_SIZE
) : DraftRepository {
    override suspend fun getDrafts(): List<Draft> = inSession { scope, userId ->
        remote.list(scope).take(listSize).flatMap { summary ->
            val titles = remote.versions(scope, summary.id).associate { it.version to it.title.orEmpty() }
            titles.keys.sorted().map { version ->
                toDraft(
                    userId, summary.id, version, titles.getValue(version), "", summary.sourceContractId,
                    summary.sourceDraftId, summary.conversationId, summary.createdAt, summary.updatedAt
                )
            }
        }
    }

    override suspend fun getDraft(draftId: String): Draft? = inSession { scope, userId ->
        val (id, version) = RemoteDraftId.parse(draftId) ?: return@inSession null
        loadVersion(scope, userId, id, version)
    }

    /** Full text of the newest version in [stage]. */
    override suspend fun getLatestDraft(stage: DraftStage): Draft? =
        getDrafts().filter { it.stage == stage }.maxWithOrNull(compareBy<Draft> { it.updatedAt }.thenBy { it.versionNo })
            ?.let { getDraft(it.draftId) }

    override suspend fun saveDraft(
        stage: DraftStage, title: String, content: String, contractId: String?, sourceDraftId: String?,
        conversationId: String?, isSaved: Boolean, isTitleGenerated: Boolean
    ): Draft = inSession { scope, userId ->
        val source = sourceDraftId?.let(RemoteDraftId::parse)
        val saved = if (source != null) {
            // A new version of the source draft, so the earlier version stays untouched.
            val current = remote.get(scope, source.first)
            remote.update(scope, source.first, UpdateDraftRequest(current.version, title, content))
        } else {
            remote.create(scope, CreateDraftRequest(title, content, contractId?.toLongOrNull(), conversationId?.toLongOrNull()))
        }
        marks.put(userId, saved.id, saved.version, DraftVersionMark(stage, isSaved))
        loadVersion(scope, userId, saved.id, saved.version)
    }

    override suspend fun createCopy(sourceDraftId: String, title: String, stage: DraftStage, isTitleGenerated: Boolean): Draft? =
        inSession { scope, userId ->
            val (sourceId, sourceVersion) = RemoteDraftId.parse(sourceDraftId) ?: return@inSession null
            // The backend copies the current version, so an older version is copied by its text instead.
            val source = loadVersion(scope, userId, sourceId, sourceVersion)
            val current = remote.get(scope, sourceId)
            var saved = if (current.version == sourceVersion) remote.copy(scope, sourceId)
            else remote.create(scope, CreateDraftRequest(source.title, source.content, current.sourceContractId, current.conversationId))
            if (title.isNotBlank() && title != source.title) {
                saved = remote.update(scope, saved.id, UpdateDraftRequest(saved.version, title = title))
            }
            marks.put(userId, saved.id, saved.version, DraftVersionMark(stage, isSaved = false))
            loadVersion(scope, userId, saved.id, saved.version)
        }

    /** Only the newest version can change; an older one returns null. */
    override suspend fun updateDraft(draftId: String, title: String, content: String): Draft? = inSession { scope, userId ->
        val (id, version) = RemoteDraftId.parse(draftId) ?: return@inSession null
        val previous = marks.get(userId, id, version)
        val saved = try {
            remote.update(scope, id, UpdateDraftRequest(version, title, content))
        } catch (error: HttpException) {
            error.response()?.errorBody()?.close()
            if (ApiFailureMapper.map(error).statusCode == 409) return@inSession null
            throw error
        }
        marks.put(userId, saved.id, saved.version, previous ?: DraftVersionMark(DraftStage.MANUAL_EDIT, isSaved = false))
        loadVersion(scope, userId, saved.id, saved.version)
    }

    override suspend fun deleteDraft(draftId: String): Boolean = inSession { scope, _ ->
        val (id, _) = RemoteDraftId.parse(draftId) ?: return@inSession false
        try {
            remote.delete(scope, id)
            true
        } catch (error: HttpException) {
            // Another version of this backend draft was removed first.
            if (ApiFailureMapper.map(error).statusCode == 404) true else throw error
        }
    }

    /**
     * One idempotency key per draft version and session, kept for the life of the process: tapping again, or
     * retrying after a lost response, gets the same contract back instead of a second one.
     */
    private val contractKeys = java.util.concurrent.ConcurrentHashMap<String, String>()

    override suspend fun saveAsContract(draftId: String): String? = inSession { scope, _ ->
        val (id, version) = RemoteDraftId.parse(draftId) ?: return@inSession null
        val key = contractKeys.getOrPut("${scope.key}|$id:$version") { java.util.UUID.randomUUID().toString() }
        remote.saveAsContract(scope, id, key).contractId.toString()
    }

    internal suspend fun loadVersion(scope: AuthSessionScope, userId: String, id: Long, version: Int): Draft {
        val draft = remote.get(scope, id)
        val text: DraftVersionResponse? = if (draft.version == version) null else remote.version(scope, id, version)
        return toDraft(
            userId, id, version, text?.title ?: draft.title.orEmpty(), (text?.content ?: draft.content).orEmpty(),
            draft.sourceContractId, draft.sourceDraftId, draft.conversationId, draft.createdAt, text?.createdAt ?: draft.updatedAt
        )
    }

    private fun toDraft(
        userId: String, id: Long, version: Int, title: String, content: String, contractId: Long?,
        sourceDraftId: Long?, conversationId: Long?, createdAt: String?, updatedAt: String?
    ): Draft {
        val mark = marks.get(userId, id, version)
        // Without a mark the first version is the original and later ones are edits; a copy starts as a copy.
        val stage = mark?.stage ?: when {
            sourceDraftId != null -> DraftStage.COPY
            version <= FIRST_VERSION + 1 -> DraftStage.ORIGINAL
            else -> DraftStage.MANUAL_EDIT
        }
        return Draft(
            draftId = RemoteDraftId.of(id, version),
            contractId = contractId?.toString(),
            title = title,
            content = content,
            stage = stage,
            versionNo = version,
            createdAt = epoch(createdAt),
            updatedAt = epoch(updatedAt),
            parentDraftId = if (version > FIRST_VERSION) RemoteDraftId.of(id, version - 1) else sourceDraftId?.let { RemoteDraftId.of(it, FIRST_VERSION) },
            lineageId = id.toString(),
            conversationId = conversationId?.toString(),
            isSaved = mark?.isSaved ?: false,
            isTitleGenerated = true
        )
    }

    private fun epoch(value: String?): Long = value?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() } ?: 0L

    private suspend fun <T> inSession(action: suspend (AuthSessionScope, String) -> T): T = withContext(Dispatchers.IO) {
        val scope = checkNotNull(tokens.currentSessionScope()) { "No active session" }
        val userId = checkNotNull(tokens.userId()) { "No active session" }
        val result = action(scope, userId)
        check(tokens.currentSessionScope()?.key == scope.key) { "Session changed" }
        result
    }

    private companion object {
        const val FIRST_VERSION = 1
        const val LIST_SIZE = 20
    }
}
