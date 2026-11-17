package com.example.lagallens.data.repository

import com.example.lagallens.domain.repository.CreditRepository
import com.example.lagallens.domain.model.credit.CreditReceipt
import com.example.lagallens.domain.model.credit.CreditTransaction
import com.example.lagallens.core.network.BackendNotConfiguredException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class UnconfiguredRemoteCreditRepository(private val sessionKey: () -> String? = { null }) : CreditRepository {
    override fun activeSessionKey(): String? = sessionKey()
    override val isConfigured: Boolean = false
    override val balance = MutableStateFlow(0).asStateFlow()
    private fun unavailable(): Nothing = throw BackendNotConfiguredException("Credit")
    override suspend fun getTransactions(): List<CreditTransaction> = unavailable()
    override suspend fun spend(transactionId: String, credits: Int): CreditReceipt = unavailable()
}
