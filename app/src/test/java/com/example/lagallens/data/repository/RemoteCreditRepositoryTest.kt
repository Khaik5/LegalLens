package com.example.lagallens.data.repository

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.core.network.BackendNotConfiguredException
import com.example.lagallens.data.datasource.remote.CreditRemoteDataSource
import com.example.lagallens.data.datasource.remote.api.CreditApi
import com.example.lagallens.data.model.response.ApiEnvelope
import com.example.lagallens.data.model.response.CreditPackageResponse
import com.example.lagallens.data.model.response.CreditTransactionPageResponse
import com.example.lagallens.data.model.response.CreditTransactionResponse
import com.example.lagallens.data.model.response.CreditWalletResponse
import com.example.lagallens.domain.model.credit.CreditOffer
import com.example.lagallens.domain.model.credit.CreditTransaction
import com.example.lagallens.domain.model.credit.CreditTransactionKind
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class RemoteCreditRepositoryTest {
    private val session = RemoteRepositorySession()
    private val api = CreditFixture()
    private val remote = CreditRemoteDataSource(lazyOf(api))
    private val credits = RemoteCreditRepository(session, remote)
    private val payments = RemotePaymentRepository(session, remote)

    @Test fun refreshLoadsTheBalanceRoundedDown() = runTest {
        api.balance = 149.75

        credits.refresh()

        assertEquals(149, credits.balance.value)
        assertTrue(credits.isConfigured)
    }

    @Test fun aFailedRefreshKeepsTheLastBalance() = runTest {
        api.balance = 20.0
        credits.refresh()
        api.walletFailure = HttpException(Response.error<Unit>(503, "".toResponseBody()))

        assertTrue(runCatching { credits.refresh() }.isFailure)
        assertEquals(20, credits.balance.value)
    }

    @Test fun anotherAccountNeverSeesThePreviousBalance() = runTest {
        api.balance = 50.0
        credits.refresh()
        session.key = "other-session"
        api.walletFailure = HttpException(Response.error<Unit>(503, "".toResponseBody()))

        runCatching { credits.refresh() }

        assertEquals(0, credits.balance.value)
    }

    @Test fun transactionsMapTheirKindAndSignedAmount() = runTest {
        api.transactions = listOf(
            transaction(3, "USAGE", -5.0, paymentId = null, createdAt = "2026-11-02T08:00:00.000Z"),
            transaction(2, "PURCHASE", 100.0, paymentId = 9, createdAt = "2026-11-01T08:00:00.000Z"),
            transaction(1, "SOMETHING_NEW", 1.0, paymentId = null, createdAt = null)
        )

        val result = credits.getTransactions()

        assertEquals(1, api.pageRequests.single())
        assertEquals(CreditApi.HISTORY_SIZE, api.limitRequests.single())
        assertEquals(
            listOf(
                CreditTransaction("3", null, null, 0, -5, "COMPLETED", 1_793_606_400_000, CreditTransactionKind.USAGE),
                CreditTransaction("2", "9", null, 0, 100, "COMPLETED", 1_793_520_000_000, CreditTransactionKind.PURCHASE),
                CreditTransaction("1", null, null, 0, 1, "COMPLETED", 0, null)
            ),
            result
        )
    }

    @Test fun spendingWaitsForTheUnlockApi() = runTest {
        val failure = runCatching { credits.spend("unlock", 2) }.exceptionOrNull()

        assertTrue(failure is BackendNotConfiguredException)
    }

    @Test fun packagesIncludeTheirBonusCredits() = runTest {
        api.packages = listOf(
            CreditPackageResponse(7, "STARTER", "Gói cơ bản", null, 50, null, 49_000, 1),
            CreditPackageResponse(8, "PRO", "Gói chuyên nghiệp", "Tặng thêm 20", 100, 20, 89_000, 2)
        )

        assertEquals(
            listOf(
                CreditOffer("7", 50, 49_000, "Gói cơ bản", ""),
                CreditOffer("8", 120, 89_000, "Gói chuyên nghiệp", "Tặng thêm 20")
            ),
            payments.getPackages()
        )
    }

    @Test fun checkoutStaysUnavailableUntilPaymentsAreEnabled() = runTest {
        assertFalse(payments.isConfigured)
        assertTrue(runCatching { payments.createPayment("p1", "7") }.exceptionOrNull() is BackendNotConfiguredException)
    }

    private fun transaction(id: Long, type: String, amount: Double, paymentId: Long?, createdAt: String?) =
        CreditTransactionResponse(id, type, amount, 0.0, paymentId, null, createdAt)

    private class CreditFixture : CreditApi {
        var balance = 0.0
        var walletFailure: Exception? = null
        var transactions = emptyList<CreditTransactionResponse>()
        var packages = emptyList<CreditPackageResponse>()
        val pageRequests = mutableListOf<Int>()
        val limitRequests = mutableListOf<Int>()

        override suspend fun wallet(scope: AuthSessionScope): ApiEnvelope<CreditWalletResponse> {
            walletFailure?.let { throw it }
            return ApiEnvelope(true, CreditWalletResponse(1, 42, balance, null))
        }

        override suspend fun transactions(scope: AuthSessionScope, page: Int, limit: Int): ApiEnvelope<CreditTransactionPageResponse> {
            pageRequests.add(page)
            limitRequests.add(limit)
            return ApiEnvelope(true, CreditTransactionPageResponse(transactions, transactions.size.toLong(), page, limit))
        }

        override suspend fun packages(scope: AuthSessionScope) = ApiEnvelope(true, packages)
    }
}
