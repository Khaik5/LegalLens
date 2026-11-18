package com.example.lagallens.data.datasource.remote

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.datasource.remote.api.OcrApi
import com.example.lagallens.data.model.request.OcrConfirmRequest
import com.example.lagallens.data.model.request.OcrCorrectionRequest
import com.example.lagallens.data.model.response.OcrResultResponse
import retrofit2.HttpException

class OcrRemoteDataSource(private val api: Lazy<OcrApi>) {
    suspend fun create(scope: AuthSessionScope, documentId: String, idempotencyKey: String) =
        api.value.create(scope, documentId, idempotencyKey).requireData("OCR job").also {
            // A new recognition makes every revision shown so far outdated.
            viewedRevisions.remove(viewKey(scope, documentId))
        }

    /** Null when OCR was never requested for this document. */
    suspend fun latest(scope: AuthSessionScope, documentId: String): OcrResultResponse? = try {
        api.value.latest(scope, documentId).requireData("OCR result")
    } catch (error: HttpException) {
        error.response()?.errorBody()?.close()
        if (error.code() != 404) throw error
        null
    }

    /**
     * Like [latest], and remembers the revision the user is looking at. A later save sends that revision, not
     * whatever is newest at save time, so an edit made elsewhere in between is reported as a conflict instead
     * of being overwritten.
     */
    suspend fun latestForView(scope: AuthSessionScope, documentId: String): OcrResultResponse? =
        latest(scope, documentId)?.also { result -> result.revision?.let { viewedRevisions[viewKey(scope, documentId)] = it } }

    /** The revision last shown for this document, or null when none was shown in this process. */
    fun viewedRevision(scope: AuthSessionScope, documentId: String): Int? = viewedRevisions[viewKey(scope, documentId)]

    private val viewedRevisions = java.util.concurrent.ConcurrentHashMap<String, Int>()
    private fun viewKey(scope: AuthSessionScope, documentId: String) = "${scope.key}/$documentId"

    /** The backend answers 409 or 404 when no job is active; that is not a failure for the caller. */
    suspend fun cancel(scope: AuthSessionScope, documentId: String) {
        val response = api.value.cancel(scope, documentId)
        response.errorBody()?.close()
        if (!response.isSuccessful && response.code() != 404 && response.code() != 409) throw HttpException(response)
    }

    suspend fun confirm(scope: AuthSessionScope, documentId: String, expectedRevision: Int) =
        api.value.confirm(scope, documentId, OcrConfirmRequest(expectedRevision)).requireData("OCR confirmation")

    suspend fun correct(scope: AuthSessionScope, documentId: String, expectedRevision: Int, text: String) =
        api.value.correct(scope, documentId, OcrCorrectionRequest(expectedRevision, text)).requireData("OCR correction")
            .also { viewedRevisions[viewKey(scope, documentId)] = it.revision }
}
