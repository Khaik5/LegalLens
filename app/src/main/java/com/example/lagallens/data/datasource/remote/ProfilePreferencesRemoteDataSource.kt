package com.example.lagallens.data.datasource.remote

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.datasource.remote.api.ProfilePreferencesApi
import com.example.lagallens.data.model.request.PatchProfilePreferencesRequest

class ProfilePreferencesRemoteDataSource(private val api: Lazy<ProfilePreferencesApi>) {
    suspend fun get(scope: AuthSessionScope) = api.value.get(scope).requireData("Profile preferences")

    suspend fun patch(scope: AuthSessionScope, request: PatchProfilePreferencesRequest) =
        api.value.patch(scope, request).requireData("Profile preferences update")
}
