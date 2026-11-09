package com.example.lagallens.data.repository

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class NotificationSessionSafetyTest {
    private class Store : NotificationReadStore {
        var session = "A:1"
        private val accounts = mutableMapOf<String, Set<String>>()
        override fun getReadIds() = accounts[session.substringBefore(':')].orEmpty()
        override fun updateReadIds(transform: (Set<String>) -> Set<String>) {
            accounts[session.substringBefore(':')] = transform(getReadIds())
        }
        override fun <T> inSession(sessionKey: String?, action: () -> T): T {
            check(session == sessionKey)
            return action()
        }
    }

    @Test fun staleWriteCannotChangeOtherAccountOrNewGeneration() = runTest {
        val store = Store()
        val repo = FakeNotificationRepository(store)
        repo.markReadForSession("A:1", "a")
        store.session = "B:2"
        assertThrows(IllegalStateException::class.java) { kotlinx.coroutines.runBlocking { repo.markReadForSession("A:1", "old") } }
        assertEquals(emptySet<String>(), store.getReadIds())
        store.session = "A:3"
        assertThrows(IllegalStateException::class.java) { kotlinx.coroutines.runBlocking { repo.markAllReadForSession("A:1") } }
        assertEquals(setOf("a"), store.getReadIds())
    }
}
