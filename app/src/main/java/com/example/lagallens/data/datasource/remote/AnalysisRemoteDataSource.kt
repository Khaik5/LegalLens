package com.example.lagallens.data.datasource.remote

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.datasource.remote.api.AnalysisApi
import com.example.lagallens.data.model.request.AnalysisJobRequest
import com.example.lagallens.data.model.response.AnalysisLatestResponse
import retrofit2.HttpException

class AnalysisRemoteDataSource(private val api: Lazy<AnalysisApi>) {
    suspend fun create(scope: AuthSessionScope, idempotencyKey: String, request: AnalysisJobRequest) =
        api.value.create(scope, idempotencyKey, request).requireData("Analysis job")

    suspend fun job(scope: AuthSessionScope, jobId: String) = api.value.job(scope, jobId).requireData("Analysis job")

    suspend fun result(scope: AuthSessionScope, jobId: String) = api.value.result(scope, jobId).requireData("Analysis result")

    suspend fun unlockStatus(scope: AuthSessionScope, jobId: String) =
        api.value.unlockStatus(scope, jobId).requireData("Analysis unlock status")

    suspend fun unlock(scope: AuthSessionScope, jobId: String) = api.value.unlock(scope, jobId).requireData("Analysis unlock")

    /** Null when the contract has no analysis yet. */
    suspend fun latest(scope: AuthSessionScope, contractId: String): AnalysisLatestResponse? = try {
        api.value.latest(scope, contractId).requireData("Latest analysis")
    } catch (error: HttpException) {
        error.response()?.errorBody()?.close()
        if (error.code() != 404) throw error
        null
    }
}
