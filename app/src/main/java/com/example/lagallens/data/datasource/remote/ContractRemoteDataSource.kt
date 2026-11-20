package com.example.lagallens.data.datasource.remote

import com.example.lagallens.core.network.ApiFailureMapper
import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.core.network.EmptyApiResponseException
import com.example.lagallens.domain.model.ContractHasPaidResultsException
import com.example.lagallens.data.datasource.remote.api.ContractApi
import com.example.lagallens.data.model.request.CompareContractsRequest
import com.example.lagallens.data.model.request.CreateContractRequest
import com.example.lagallens.data.model.request.UpdateContractRequest
import com.example.lagallens.data.model.response.ContractComparisonResponse
import com.example.lagallens.data.model.response.ContractResponse
import retrofit2.HttpException

private const val CONTRACT_HAS_PAID_RESULTS = "CONTRACT_HAS_PAID_RESULTS"

class ContractRemoteDataSource(private val api: Lazy<ContractApi>) {
    suspend fun list(scope: AuthSessionScope, userId: String): List<ContractResponse> {
        val contracts = mutableListOf<ContractResponse>()
        var page = 1
        while (true) {
            val response = api.value.list(scope, userId, page).requireData("Contracts")
            contracts.addAll(response.items)
            if (response.items.isEmpty() || page.toLong() * response.limit >= response.total) break
            check(response.limit > 0) { "Invalid contract page size" }
            page++
        }
        return contracts.distinctBy { it.id }
    }

    suspend fun get(scope: AuthSessionScope, id: String): ContractResponse? = try {
        getRequired(scope, id)
    } catch (error: HttpException) {
        if (error.code() != 403 && error.code() != 404) throw error
        error.response()?.errorBody()?.close()
        null
    }

    suspend fun getRequired(scope: AuthSessionScope, id: String): ContractResponse =
        api.value.get(scope, id).requireData("Contract")

    suspend fun create(scope: AuthSessionScope, title: String, description: String?) =
        api.value.create(scope, CreateContractRequest(title, description)).requireData("Contract creation")

    suspend fun rename(scope: AuthSessionScope, id: String, title: String, description: String?) =
        api.value.update(scope, id, UpdateContractRequest(title, description)).requireData("Contract rename")

    suspend fun delete(scope: AuthSessionScope, id: String, confirmPaidResults: Boolean = false) {
        try {
            val response = api.value.delete(scope, id, confirmPaidResults.takeIf { it })
            if (!response.success) throw EmptyApiResponseException("Contract deletion")
        } catch (error: HttpException) {
            if (error.code() == 404) {
                error.response()?.errorBody()?.close()
                return
            }
            if (error.code() == 409 && ApiFailureMapper.map(error).details?.code == CONTRACT_HAS_PAID_RESULTS) {
                throw ContractHasPaidResultsException(error)
            }
            throw error
        }
    }

    suspend fun compare(scope: AuthSessionScope, firstId: Long, secondId: Long): ContractComparisonResponse =
        api.value.compare(scope, CompareContractsRequest(firstId, secondId)).requireData("Contract comparison")
}
