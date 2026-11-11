package com.example.lagallens.data.datasource.remote

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.datasource.remote.api.UserApi
import com.example.lagallens.data.model.request.UpdateMyProfileRequest
import com.google.gson.GsonBuilder
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.HttpException

class UserRemoteDataSource(private val api: Lazy<UserApi>) {
    suspend fun me(scope: AuthSessionScope) = api.value.me(scope).requireData("Profile")

    suspend fun updateMe(scope: AuthSessionScope, request: UpdateMyProfileRequest) =
        api.value.updateMe(scope, nullPreservingJson.toJson(request).toRequestBody(JSON))
            .requireData("Profile update")

    suspend fun uploadAvatar(scope: AuthSessionScope, jpeg: ByteArray) = api.value.uploadAvatar(
        scope,
        MultipartBody.Part.createFormData("file", "avatar.jpg", jpeg.toRequestBody(JPEG))
    ).requireData("Avatar upload")

    /** Returns null when the account has no avatar. The caller closes the body. */
    suspend fun avatar(scope: AuthSessionScope): ResponseBody? = try {
        api.value.avatar(scope)
    } catch (error: HttpException) {
        error.response()?.errorBody()?.close()
        if (error.code() != 404) throw error
        null
    }

    private companion object {
        val nullPreservingJson = GsonBuilder().serializeNulls().create()
        val JSON = "application/json; charset=utf-8".toMediaType()
        val JPEG = "image/jpeg".toMediaType()
    }
}
