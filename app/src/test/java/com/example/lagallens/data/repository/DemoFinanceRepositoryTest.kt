package com.example.lagallens.data.repository

import com.example.lagallens.data.datasource.local.datastore.DemoSessionScope
import com.example.lagallens.domain.model.payment.PaymentStatus
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class DemoFinanceRepositoryTest {
    private class Store : FinanceStorage {
        val records = mutableMapOf<String, FinanceSnapshot>()
        var failBefore = false
        var failAfter = false
        override fun read(userId: String) = records[userId]
        override fun write(userId: String, snapshot: FinanceSnapshot) {
            if (failBefore) throw java.io.IOException("before commit")
            records[userId] = snapshot
            if (failAfter) throw java.io.IOException("after commit")
        }
    }
    private class Fixture {
        val lock = Any()
        val store = Store()
        var session: DemoSessionScope? = DemoSessionScope("A", "1")
        var switchBeforeCommit = false
        fun repository() = DemoFinanceRepository(store, { synchronized(lock) { session } }, { scope, action ->
            synchronized(lock) {
                if (switchBeforeCommit) { session = DemoSessionScope("B", "2"); switchBeforeCommit = false }
                check(session == scope)
                action()
            }
        }, { 123L })
    }

    @Test fun accountsAndZeroBalancesPersistAcrossLogoutAndRestart() = runBlocking {
        val f = Fixture(); val repo = f.repository()
        assertTrue(repo.spend("unlock", 5).applied)
        f.session = DemoSessionScope("B", "2")
        repo.refreshSession(); assertEquals(5, repo.balance.value)
        assertTrue(repo.getTransactions().isEmpty())
        f.session = null; repo.refreshSession(); assertEquals(0, repo.balance.value)
        try { repo.spend("x", 1); fail() } catch (_: IllegalStateException) { }
        f.session = DemoSessionScope("A", "3")
        val reopened = f.repository(); reopened.refreshSession()
        assertEquals(0, reopened.balance.value)
        assertFalse(reopened.spend("other", 1).applied)
        assertFalse(reopened.spend("unlock", 5).applied)
        assertEquals(1, reopened.getTransactions().size)
    }

    @Test fun paymentIsolationConflictsAndTerminalStates() = runBlocking {
        val f = Fixture(); val repo = f.repository()
        val pending = repo.createOrGet("p", "pack", 1000, 3)
        assertEquals(pending, repo.createOrGet("p", "pack", 1000, 3))
        try { repo.createOrGet("p", "other", 1000, 3); fail() } catch (_: IllegalArgumentException) { }
        repo.updateStatus("p", PaymentStatus.FAILED)
        assertEquals(PaymentStatus.FAILED, repo.completeDemoPayment("p").status)
        repo.createOrGet("retry", "pack", 1000, 3)
        repo.completeDemoPayment("retry"); repo.completeDemoPayment("retry")
        assertEquals(8, repo.balance.value)
        repo.createOrGet("cancel", "pack", 1000, 3)
        repo.updateStatus("cancel", PaymentStatus.CANCELLED)
        repo.completeDemoPayment("cancel"); assertEquals(8, repo.balance.value)
        f.session = DemoSessionScope("B", "2")
        assertNull(repo.get("retry")); assertTrue(repo.getTransactions().isEmpty())
        repo.createOrGet("retry", "different-account", 2000, 4)
        f.session = DemoSessionScope("A", "3")
        assertEquals(PaymentStatus.SUCCESS, repo.get("retry")?.status)
    }

    @Test fun concurrentCompletionAndUnlockApplyOnce() = runBlocking {
        val f = Fixture(); val repo = f.repository()
        repo.createOrGet("p", "pack", 1000, 3)
        (1..30).map { async(Dispatchers.Default) { repo.completeDemoPayment("p") } }.awaitAll()
        assertEquals(8, repo.balance.value); assertEquals(1, repo.getTransactions().size)
        val results = (1..30).map { async(Dispatchers.Default) { repo.spend("unlock", 2) } }.awaitAll()
        assertEquals(1, results.count { it.applied }); assertEquals(6, repo.balance.value)
        try { repo.spend("unlock", 3); fail() } catch (_: IllegalArgumentException) { }
    }

    @Test fun interruptedCommitReconcilesWithoutDoubleCredit() = runBlocking {
        val f = Fixture(); val repo = f.repository()
        repo.createOrGet("p", "pack", 1000, 3)
        f.store.failBefore = true
        try { repo.completeDemoPayment("p"); fail() } catch (_: java.io.IOException) { }
        assertEquals(PaymentStatus.PENDING, repo.get("p")?.status)
        assertTrue(repo.getTransactions().isEmpty())
        f.store.failBefore = false; f.store.failAfter = true
        try { repo.completeDemoPayment("p"); fail() } catch (_: java.io.IOException) { }
        f.store.failAfter = false
        repo.completeDemoPayment("p")
        assertEquals(8, repo.balance.value)
        val reopened = f.repository()
        reopened.completeDemoPayment("p"); reopened.refreshSession()
        assertEquals(8, reopened.balance.value); assertEquals(1, reopened.getTransactions().size)
        assertEquals(PaymentStatus.SUCCESS, reopened.get("p")?.status)
    }

    @Test fun sessionChangeBeforeExecutionCannotCreditAnotherUser() = runBlocking {
        val f = Fixture(); val repo = f.repository()
        repo.createOrGet("p", "pack", 1000, 3)
        f.switchBeforeCommit = true
        try { repo.completeDemoPayment("p"); fail() } catch (_: IllegalStateException) { }
        assertNull(repo.get("p")); assertTrue(repo.getTransactions().isEmpty())
        f.session = DemoSessionScope("A", "3")
        assertEquals(PaymentStatus.PENDING, repo.get("p")?.status)
        repo.completeDemoPayment("p"); assertEquals(8, repo.balance.value)
    }

    @Test fun realPaymentIsUnconfiguredAndCannotCompleteDemo() = runBlocking {
        val remote = UnconfiguredRemotePaymentRepository()
        assertFalse(remote.isConfigured)
        try { remote.completeDemoPayment("p"); fail() }
        catch (_: com.example.lagallens.core.network.BackendNotConfiguredException) { }
    }
}
