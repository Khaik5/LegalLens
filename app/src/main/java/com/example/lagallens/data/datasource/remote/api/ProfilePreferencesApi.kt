package com.example.lagallens.data.datasource.remote.api

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.model.request.PatchProfilePreferencesRequest
import com.example.lagallens.data.model.response.ApiEnvelope
import com.example.lagallens.data.model.response.ProfilePreferencesResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Tag

interface ProfilePreferencesApi {
    @GET("users/me/preferences")
    suspend fun get(@Tag scope: AuthSessionScope): ApiEnvelope<ProfilePreferencesResponse>

    @PATCH("users/me/preferences")
    suspend fun patch(
        @Tag scope: AuthSessionScope,
        @Body request: PatchProfilePreferencesRequest
    ): ApiEnvelope<ProfilePreferencesResponse>
}
