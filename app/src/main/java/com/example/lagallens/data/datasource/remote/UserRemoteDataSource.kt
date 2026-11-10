package com.example.lagallens.data.datasource.remote

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.datasource.remote.api.UserApi
import com.example.lagallens.data.model.request.UpdateUserRequest

class UserRemoteDataSource(private val api: Lazy<UserApi>) {
    suspend fun me(scope: AuthSessionScope) = api.value.me(scope).requireData("Profile")

    suspend fun update(scope: AuthSessionScope, userId: String, name: String) =
        api.value.update(scope, userId, UpdateUserRequest(name)).requireData("Profile update")
}
