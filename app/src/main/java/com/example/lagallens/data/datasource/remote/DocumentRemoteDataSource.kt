package com.example.lagallens.data.datasource.remote

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.core.network.EmptyApiResponseException
import com.example.lagallens.data.datasource.remote.api.DocumentApi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

class DocumentRemoteDataSource(private val api: Lazy<DocumentApi>) {
    suspend fun upload(scope: AuthSessionScope, contractId: String, fileName: String, body: RequestBody) =
        api.value.upload(
            scope,
            contractId.toRequestBody("text/plain".toMediaType()),
            MultipartBody.Part.createFormData("file", fileName, body)
        ).requireData("Document upload")

    suspend fun list(scope: AuthSessionScope, contractId: String) =
        api.value.list(scope, contractId).requireData("Contract documents")

    suspend fun get(scope: AuthSessionScope, id: String) = try {
        api.value.get(scope, id).requireData("Document")
    } catch (error: HttpException) {
        error.response()?.errorBody()?.close()
        if (error.code() != 404) throw error
        null
    }

    suspend fun delete(scope: AuthSessionScope, id: String): Boolean = try {
        if (!api.value.delete(scope, id).success) throw EmptyApiResponseException("Document deletion")
        true
    } catch (error: HttpException) {
        error.response()?.errorBody()?.close()
        if (error.code() != 404) throw error
        false
    }

    suspend fun preview(scope: AuthSessionScope, id: String) = api.value.preview(scope, id)
    suspend fun download(scope: AuthSessionScope, id: String) = api.value.download(scope, id)
}
