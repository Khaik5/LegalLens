package com.example.lagallens.data.repository

import com.example.lagallens.core.network.ApiFailureMapper
import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.datasource.local.datastore.AuthSessionStore
import com.example.lagallens.data.datasource.local.datastore.currentSessionScope
import com.example.lagallens.data.datasource.remote.CreditRemoteDataSource
import com.example.lagallens.data.datasource.remote.PaymentRemoteDataSource
import com.example.lagallens.data.mapper.toCreditOffer
import com.example.lagallens.domain.model.credit.CreditOffer
import com.example.lagallens.domain.model.payment.BankTransfer
import com.example.lagallens.domain.model.payment.BankTransferUnavailableException
import com.example.lagallens.domain.model.payment.PaymentHistoryEntry
import com.example.lagallens.domain.model.payment.PaymentRecord
import com.example.lagallens.domain.model.payment.PaymentStatus
import com.example.lagallens.domain.repository.PaymentRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Packages and bank-transfer payments (VietQR / SePay) from the backend. Payment confirmation comes from the
 * provider's webhook on the backend; the app only polls the status, so it can never credit itself.
 */
class RemotePaymentRepository(
    private val tokens: AuthSessionStore,
    private val credits: CreditRemoteDataSource,
    private val payments: PaymentRemoteDataSource,
    private val pollIntervalMillis: Long = POLL_INTERVAL_MILLIS,
    private val now: () -> Long = System::currentTimeMillis
) : PaymentRepository {
    /** The app names a payment before the backend does, and its screens keep using the app's ID. */
    private val backendIds = ConcurrentHashMap<String, Long>()
    private val records = ConcurrentHashMap<String, PaymentRecord>()

    override fun activeSessionKey(): String? = tokens.currentSessionScope()?.key

    override suspend fun getPackages(): List<CreditOffer> = withContext(Dispatchers.IO) {
        val scope = checkNotNull(tokens.currentSessionScope()) { "No active session" }
        val packages = credits.packages(scope).map { it.toCreditOffer() }
        check(tokens.currentSessionScope()?.key == scope.key) { "Session changed" }
        packages
    }

    /** The app's payment ID is the idempotency key, so repeating the call never opens a second payment. */
    override suspend fun createPayment(paymentId: String, packageId: String): PaymentRecord = withContext(Dispatchers.IO) {
        val scope = requireScope()
        val offerId = requireNotNull(packageId.toLongOrNull()) { "Invalid package" }
        val created = payments.initiate(scope, paymentId, offerId)
        backendIds[paymentId] = created.paymentId
        // The payment exists on the backend now; a failed package lookup must not turn it into a reported failure.
        val offer = try {
            getPackages().firstOrNull { it.id == packageId }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        PaymentRecord(
            paymentId = paymentId,
            packageId = packageId,
            amountVnd = offer?.priceVnd ?: 0L,
            credits = offer?.credits ?: 0,
            status = mapStatus(created.status),
            updatedAtEpochMillis = now()
        ).also { records[paymentId] = it }
    }

    override suspend fun getBankTransfer(paymentId: String): BankTransfer? = withContext(Dispatchers.IO) {
        val backendId = backendIds[paymentId] ?: return@withContext null
        try {
            val transfer = payments.bankTransfer(requireScope(), backendId)
            BankTransfer(
                paymentId = paymentId,
                amountVnd = transfer.amountVnd,
                paymentCode = transfer.paymentCode,
                qrUrl = transfer.qrUrl,
                expiresAtEpochMillis = transfer.expiresAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: HttpException) {
            val code = ApiFailureMapper.map(error).statusCode
            // 503: no receiving account configured; 409: the payment can no longer be paid by transfer.
            if (code == 503 || code == 409) throw BankTransferUnavailableException()
            throw error
        }
    }

    /** Polls until the backend reports a final status; short network drops are tolerated. */
    override fun observePayment(paymentId: String): Flow<PaymentRecord> = flow {
        val backendId = checkNotNull(backendIds[paymentId]) { "Unknown payment" }
        val scope = requireScope()
        var networkFailures = 0
        while (true) {
            val status = try {
                mapStatus(payments.status(scope, backendId).status).also { networkFailures = 0 }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: IOException) {
                if (++networkFailures > MAX_NETWORK_FAILURES) throw error
                delay(pollIntervalMillis)
                continue
            } catch (error: HttpException) {
                // A gateway or rate-limit answer says nothing about the payment; it may already be paid.
                if (!error.isTransient() || ++networkFailures > MAX_NETWORK_FAILURES) throw error
                delay(pollIntervalMillis)
                continue
            }
            var record = (records[paymentId] ?: error("Unknown payment")).copy(status = status, updatedAtEpochMillis = now())
            if (status == PaymentStatus.SUCCESS && record.credits == 0) record = withPurchaseDetails(scope, backendId, record)
            records[paymentId] = record
            emit(record)
            if (status != PaymentStatus.PENDING) return@flow
            delay(pollIntervalMillis)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun cancelPayment(paymentId: String): PaymentRecord = withContext(Dispatchers.IO) {
        val backendId = checkNotNull(backendIds[paymentId]) { "Unknown payment" }
        val status = mapStatus(payments.cancel(requireScope(), backendId).status)
        (records[paymentId] ?: error("Unknown payment")).copy(status = status, updatedAtEpochMillis = now())
            .also { records[paymentId] = it }
    }

    override suspend fun get(paymentId: String): PaymentRecord? = records[paymentId]

    override suspend fun getHistory(): List<PaymentHistoryEntry> = withContext(Dispatchers.IO) {
        val scope = requireScope()
        val entries = payments.history(scope).map { item ->
            PaymentHistoryEntry(
                paymentId = item.id.toString(),
                credits = item.creditAmount,
                amountVnd = item.amountVnd,
                status = mapStatus(item.status),
                createdAtEpochMillis = item.createdAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() } ?: 0L
            )
        }
        check(tokens.currentSessionScope()?.key == scope.key) { "Session changed" }
        entries
    }

    private fun requireScope(): AuthSessionScope = checkNotNull(tokens.currentSessionScope()) { "No active session" }

    /** A refunded payment is shown as cancelled. */
    private fun mapStatus(status: String?): PaymentStatus = when (status?.uppercase()) {
        "SUCCESS" -> PaymentStatus.SUCCESS
        "FAILED" -> PaymentStatus.FAILED
        "CANCELLED", "REFUNDED" -> PaymentStatus.CANCELLED
        else -> PaymentStatus.PENDING
    }

    /** Credits and price are unknown when the package lookup failed at creation; the history has them for a paid payment. */
    private suspend fun withPurchaseDetails(scope: AuthSessionScope, backendId: Long, record: PaymentRecord): PaymentRecord = try {
        payments.history(scope).firstOrNull { it.id == backendId }
            ?.let { record.copy(credits = it.creditAmount, amountVnd = it.amountVnd) }
            ?: record
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        record
    }

    private fun HttpException.isTransient(): Boolean = code() == 408 || code() == 429 || code() >= 500

    private companion object {
        const val POLL_INTERVAL_MILLIS = 4_000L
        const val MAX_NETWORK_FAILURES = 3
    }
}
