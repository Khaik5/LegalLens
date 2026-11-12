package com.example.lagallens.data.datasource.remote

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.datasource.remote.api.OcrApi
import com.example.lagallens.data.model.request.OcrCorrectionRequest
import com.example.lagallens.data.model.response.OcrResultResponse
import retrofit2.HttpException

class OcrRemoteDataSource(private val api: Lazy<OcrApi>) {
    suspend fun create(scope: AuthSessionScope, documentId: String, idempotencyKey: String) =
        api.value.create(scope, documentId, idempotencyKey).requireData("OCR job")

    /** Null when OCR was never requested for this document. */
    suspend fun latest(scope: AuthSessionScope, documentId: String): OcrResultResponse? = try {
        api.value.latest(scope, documentId).requireData("OCR result")
    } catch (error: HttpException) {
        error.response()?.errorBody()?.close()
        if (error.code() != 404) throw error
        null
    }

    suspend fun correct(scope: AuthSessionScope, documentId: String, expectedRevision: Int, text: String) =
        api.value.correct(scope, documentId, OcrCorrectionRequest(expectedRevision, text)).requireData("OCR correction")
}
