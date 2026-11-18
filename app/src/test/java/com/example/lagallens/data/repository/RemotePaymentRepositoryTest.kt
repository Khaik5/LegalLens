package com.example.lagallens.data.repository

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.datasource.remote.CreditRemoteDataSource
import com.example.lagallens.data.datasource.remote.PaymentRemoteDataSource
import com.example.lagallens.data.datasource.remote.api.CreditApi
import com.example.lagallens.data.datasource.remote.api.PaymentApi
import com.example.lagallens.data.model.request.InitiatePackagePaymentRequest
import com.example.lagallens.data.model.response.ApiEnvelope
import com.example.lagallens.data.model.response.BankTransferResponse
import com.example.lagallens.data.model.response.CreditPackageResponse
import com.example.lagallens.data.model.response.CreditTransactionPageResponse
import com.example.lagallens.data.model.response.CreditWalletResponse
import com.example.lagallens.data.model.response.PackagePaymentResponse
import com.example.lagallens.data.model.response.PaymentItemResponse
import com.example.lagallens.data.model.response.PaymentPageResponse
import com.example.lagallens.data.model.response.PaymentStatusResponse
import com.example.lagallens.domain.model.payment.BankTransferUnavailableException
import com.example.lagallens.domain.model.payment.PaymentStatus
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class RemotePaymentRepositoryTest {
    private val api = PaymentFixture()
    private val packages = PackagesFixture()
    private val repository = RemotePaymentRepository(
        RemoteRepositorySession(), CreditRemoteDataSource(lazyOf(packages)), PaymentRemoteDataSource(lazyOf(api)),
        pollIntervalMillis = 1, now = { 1_000L }
    )

    @Test fun historyKeepsTheServerOrderAndMapsEveryStatus() = runTest {
        api.history = listOf(
            PaymentItemResponse(3, 15, 79_000, "PENDING", "2026-11-17T01:00:00Z"),
            PaymentItemResponse(2, 5, 29_000, "SUCCESS", "2026-11-16T01:00:00Z"),
            PaymentItemResponse(1, 5, 29_000, "REFUNDED", null)
        )

        val history = repository.getHistory()

        assertEquals(listOf("3", "2", "1"), history.map { it.paymentId })
        assertEquals(listOf(PaymentStatus.PENDING, PaymentStatus.SUCCESS, PaymentStatus.CANCELLED), history.map { it.status })
        assertEquals(79_000L, history.first().amountVnd)
        assertEquals(java.time.Instant.parse("2026-11-17T01:00:00Z").toEpochMilli(), history.first().createdAtEpochMillis)
        assertEquals(0L, history.last().createdAtEpochMillis)
    }

    @Test fun theAppPaymentIdIsTheIdempotencyKeyAndTheRecordUsesThePackage() = runTest {
        val record = repository.createPayment("pay-1", "7")

        assertEquals("pay-1", api.idempotencyKeys.single())
        assertEquals(7L, api.requests.single().packageId)
        assertEquals("BANK_TRANSFER", api.requests.single().paymentMethod)
        assertEquals(49_000L, record.amountVnd)
        assertEquals(55, record.credits)
        assertEquals(PaymentStatus.PENDING, record.status)
    }

    @Test fun transferInstructionsUseTheBackendPaymentAndExpiry() = runTest {
        repository.createPayment("pay-1", "7")

        val transfer = repository.getBankTransfer("pay-1")!!

        assertEquals(900L, api.transferRequestedFor)
        assertEquals("LL900", transfer.paymentCode)
        assertEquals(49_000L, transfer.amountVnd)
        assertEquals(1_800_000_000_000L, transfer.expiresAtEpochMillis)
    }

    @Test fun anUnconfiguredReceivingAccountIsReportedAsUnavailable() = runTest {
        repository.createPayment("pay-1", "7")
        api.transferFailure = 503

        val failure = runCatching { repository.getBankTransfer("pay-1") }.exceptionOrNull()

        assertEquals(BankTransferUnavailableException::class, failure!!::class)
    }

    @Test fun anUnknownPaymentHasNoTransfer() = runTest {
        assertNull(repository.getBankTransfer("never-created"))
    }

    @Test fun pollingEndsWhenTheBackendConfirmsPayment() = runTest {
        repository.createPayment("pay-1", "7")
        api.statuses = mutableListOf("PENDING", "PENDING", "SUCCESS")

        val states = repository.observePayment("pay-1").toList().map { it.status }

        assertEquals(listOf(PaymentStatus.PENDING, PaymentStatus.PENDING, PaymentStatus.SUCCESS), states)
    }

    @Test fun aRefundIsShownAsCancelled() = runTest {
        repository.createPayment("pay-1", "7")
        api.statuses = mutableListOf("REFUNDED")

        assertEquals(PaymentStatus.CANCELLED, repository.observePayment("pay-1").toList().last().status)
    }

    @Test fun cancellingReportsTheBackendStatus() = runTest {
        repository.createPayment("pay-1", "7")
        api.cancelStatus = "CANCELLED"

        assertEquals(PaymentStatus.CANCELLED, repository.cancelPayment("pay-1").status)
        assertEquals(PaymentStatus.CANCELLED, repository.get("pay-1")?.status)
    }

    @Test fun aPaymentLostFromMemoryIsFoundAgainByItsPurchaseId() = runTest {
        repository.createPayment("pay-1", "7")
        // The process was killed: a new repository knows nothing, the backend still has the payment.
        val afterRestart = RemotePaymentRepository(
            RemoteRepositorySession(), CreditRemoteDataSource(lazyOf(packages)), PaymentRemoteDataSource(lazyOf(api)),
            pollIntervalMillis = 1, now = { 1_000L }
        )
        assertNull(afterRestart.get("pay-1"))
        api.statuses = mutableListOf("SUCCESS")

        val recovered = afterRestart.createPayment("pay-1", "7")
        val paid = afterRestart.observePayment("pay-1").toList().last()

        assertEquals(listOf("pay-1", "pay-1"), api.idempotencyKeys)
        assertEquals(PaymentStatus.PENDING, recovered.status)
        assertEquals(PaymentStatus.SUCCESS, paid.status)
        assertEquals(55, paid.credits)
    }

    @Test fun aGatewayErrorWhilePollingIsRetriedInsteadOfEndingThePayment() = runTest {
        repository.createPayment("pay-1", "7")
        api.statusFailures = mutableListOf(502, 429)
        api.statuses = mutableListOf("SUCCESS")

        val states = repository.observePayment("pay-1").toList().map { it.status }

        assertEquals(listOf(PaymentStatus.SUCCESS), states)
    }

    @Test fun aPersistentGatewayErrorStillEndsThePoll() = runTest {
        repository.createPayment("pay-1", "7")
        api.statusFailures = mutableListOf(502, 502, 502, 502, 502)

        val failure = runCatching { repository.observePayment("pay-1").toList() }.exceptionOrNull()

        assertEquals(HttpException::class, failure!!::class)
    }

    @Test fun aClientErrorWhilePollingIsNotRetried() = runTest {
        repository.createPayment("pay-1", "7")
        api.statusFailures = mutableListOf(404)

        val failure = runCatching { repository.observePayment("pay-1").toList() }.exceptionOrNull() as HttpException

        assertEquals(404, failure.code())
        assertEquals(1, api.statusCalls)
    }

    @Test fun aFailedPackageLookupDoesNotFailTheCreatedPaymentAndCreditsComeFromHistory() = runTest {
        packages.failing = true
        api.history = listOf(PaymentItemResponse(900, 55, 49_000, "SUCCESS", "2026-11-17T01:00:00Z"))
        api.statuses = mutableListOf("SUCCESS")

        val created = repository.createPayment("pay-1", "7")
        val paid = repository.observePayment("pay-1").toList().last()

        assertEquals(PaymentStatus.PENDING, created.status)
        assertEquals(0, created.credits)
        assertEquals(55, paid.credits)
        assertEquals(49_000L, paid.amountVnd)
    }

    private class PaymentFixture : PaymentApi {
        var statusFailures = mutableListOf<Int>()
        var statusCalls = 0
        val idempotencyKeys = mutableListOf<String>()
        val requests = mutableListOf<InitiatePackagePaymentRequest>()
        var transferRequestedFor: Long? = null
        var transferFailure: Int? = null
        var statuses = mutableListOf("PENDING")
        var cancelStatus = "CANCELLED"
        private var reads = 0

        override suspend fun initiate(scope: AuthSessionScope, idempotencyKey: String, request: InitiatePackagePaymentRequest): ApiEnvelope<PackagePaymentResponse> {
            idempotencyKeys += idempotencyKey
            requests += request
            return ApiEnvelope(true, PackagePaymentResponse(900, "PENDING"))
        }

        override suspend fun bankTransfer(scope: AuthSessionScope, paymentId: Long): ApiEnvelope<BankTransferResponse> {
            transferRequestedFor = paymentId
            transferFailure?.let { throw HttpException(Response.error<Any>(it, "{}".toResponseBody())) }
            return ApiEnvelope(true, BankTransferResponse(paymentId, 49_000, "LL900", "https://qr.example/img", "2027-01-15T08:00:00Z"))
        }

        var history = listOf<PaymentItemResponse>()

        override suspend fun list(scope: AuthSessionScope, page: Int, limit: Int) =
            ApiEnvelope(true, PaymentPageResponse(history, history.size.toLong(), page, limit))

        override suspend fun status(scope: AuthSessionScope, paymentId: Long): ApiEnvelope<PaymentStatusResponse> {
            statusCalls++
            if (statusFailures.isNotEmpty()) {
                throw HttpException(Response.error<Any>(statusFailures.removeAt(0), "{}".toResponseBody()))
            }
            return ApiEnvelope(true, PaymentStatusResponse(paymentId, statuses[minOf(reads++, statuses.lastIndex)]))
        }

        override suspend fun cancel(scope: AuthSessionScope, paymentId: Long) =
            ApiEnvelope(true, PaymentStatusResponse(paymentId, cancelStatus))
    }

    private class PackagesFixture : CreditApi {
        var failing = false
        override suspend fun wallet(scope: AuthSessionScope): ApiEnvelope<CreditWalletResponse> = error("unused")
        override suspend fun transactions(scope: AuthSessionScope, page: Int, limit: Int): ApiEnvelope<CreditTransactionPageResponse> = error("unused")
        override suspend fun packages(scope: AuthSessionScope): ApiEnvelope<List<CreditPackageResponse>> {
            if (failing) throw java.io.IOException("offline")
            return ApiEnvelope(true, listOf(CreditPackageResponse(7, "BASIC", "Gói cơ bản", null, 50, 5, 49_000, 1)))
        }
    }
}
