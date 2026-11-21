package com.example.lagallens.data.datasource.remote

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.datasource.remote.api.DraftApi
import com.example.lagallens.data.model.request.CreateDraftRequest
import com.example.lagallens.data.model.request.DraftAiJobRequest
import com.example.lagallens.data.model.request.UpdateDraftRequest
import retrofit2.HttpException

class DraftRemoteDataSource(private val api: Lazy<DraftApi>) {
    suspend fun list(scope: AuthSessionScope) = api.value.list(scope).requireData("Drafts").data
    suspend fun create(scope: AuthSessionScope, request: CreateDraftRequest) = api.value.create(scope, request).requireData("Draft")
    suspend fun get(scope: AuthSessionScope, draftId: Long) = api.value.get(scope, draftId).requireData("Draft")
    suspend fun update(scope: AuthSessionScope, draftId: Long, request: UpdateDraftRequest) =
        api.value.update(scope, draftId, request).requireData("Draft update")

    suspend fun delete(scope: AuthSessionScope, draftId: Long) {
        val response = api.value.delete(scope, draftId)
        if (!response.isSuccessful) throw HttpException(response)
    }

    suspend fun copy(scope: AuthSessionScope, draftId: Long) = api.value.copy(scope, draftId).requireData("Draft copy")
    suspend fun saveAsContract(scope: AuthSessionScope, draftId: Long, key: String) =
        api.value.saveAsContract(scope, draftId, key).requireData("Draft contract")
    suspend fun versions(scope: AuthSessionScope, draftId: Long) = api.value.versions(scope, draftId).requireData("Draft versions").data
    suspend fun version(scope: AuthSessionScope, draftId: Long, version: Int) =
        api.value.version(scope, draftId, version).requireData("Draft version")

    suspend fun createJob(scope: AuthSessionScope, key: String, request: DraftAiJobRequest) =
        api.value.createJob(scope, key, request).requireData("Draft AI job")

    suspend fun job(scope: AuthSessionScope, jobId: String) = api.value.job(scope, jobId).requireData("Draft AI job")
    suspend fun apply(scope: AuthSessionScope, jobId: String) = api.value.apply(scope, jobId).requireData("Draft AI apply")
    suspend fun cancel(scope: AuthSessionScope, jobId: String) = api.value.cancel(scope, jobId).requireData("Draft AI cancel")
    suspend fun retry(scope: AuthSessionScope, jobId: String) = api.value.retry(scope, jobId).requireData("Draft AI retry")
}
