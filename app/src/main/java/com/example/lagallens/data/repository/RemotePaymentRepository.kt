package com.example.lagallens.data.repository

import com.example.lagallens.core.network.BackendNotConfiguredException
import com.example.lagallens.data.datasource.local.datastore.AuthSessionStore
import com.example.lagallens.data.datasource.local.datastore.currentSessionScope
import com.example.lagallens.data.datasource.remote.CreditRemoteDataSource
import com.example.lagallens.data.mapper.toCreditOffer
import com.example.lagallens.domain.model.credit.CreditOffer
import com.example.lagallens.domain.model.payment.PaymentRecord
import com.example.lagallens.domain.repository.PaymentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Packages come from the backend. Checkout stays unavailable ([isConfigured] is false)
 * until the SePay payment flow is enabled on the backend.
 */
class RemotePaymentRepository(
    private val tokens: AuthSessionStore,
    private val remote: CreditRemoteDataSource
) : PaymentRepository {
    override fun activeSessionKey(): String? = tokens.currentSessionScope()?.key
    override val isConfigured: Boolean = false

    override suspend fun getPackages(): List<CreditOffer> = withContext(Dispatchers.IO) {
        val scope = checkNotNull(tokens.currentSessionScope()) { "No active session" }
        val packages = remote.packages(scope).map { it.toCreditOffer() }
        check(tokens.currentSessionScope()?.key == scope.key) { "Session changed" }
        packages
    }

    private fun unavailable(): Nothing = throw BackendNotConfiguredException("Payment")
    override suspend fun createPayment(paymentId: String, packageId: String): PaymentRecord = unavailable()
    override fun observePayment(paymentId: String): Flow<PaymentRecord> = unavailable()
    override suspend fun cancelPayment(paymentId: String): PaymentRecord = unavailable()
    override suspend fun get(paymentId: String): PaymentRecord? = unavailable()
}
