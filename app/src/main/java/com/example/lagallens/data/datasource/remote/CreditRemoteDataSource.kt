package com.example.lagallens.data.datasource.remote

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.datasource.remote.api.CreditApi

class CreditRemoteDataSource(private val api: Lazy<CreditApi>) {
    suspend fun wallet(scope: AuthSessionScope) = api.value.wallet(scope).requireData("Credit wallet")

    suspend fun recentTransactions(scope: AuthSessionScope) =
        api.value.transactions(scope).requireData("Credit transactions").data

    suspend fun packages(scope: AuthSessionScope) = api.value.packages(scope).requireData("Credit packages")
}
