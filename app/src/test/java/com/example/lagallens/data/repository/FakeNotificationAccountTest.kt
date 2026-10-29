package com.example.lagallens.data.repository

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class FakeNotificationAccountTest {
    @Test fun readStateBelongsToActiveAccount() = runBlocking {
        var account: String? = "A"
        val state = mutableMapOf<String, Set<String>>()
        val store = object : NotificationReadStore {
            override fun getReadIds(): Set<String> = state[checkNotNull(account)].orEmpty()
            override fun updateReadIds(transform: (Set<String>) -> Set<String>) {
                val id = checkNotNull(account)
                state[id] = transform(state[id].orEmpty())
            }
        }
        val repository = FakeNotificationRepository(store)
        val id = repository.getAll().first { it.isUnread }.id
        repository.markRead(id)
        account = "B"
        assertTrue(repository.getAll().first { it.id == id }.isUnread)
        account = "A"
        assertFalse(repository.getAll().first { it.id == id }.isUnread)
        account = null
        try { repository.getAll(); fail("Expected missing account failure") }
        catch (_: IllegalStateException) { }
    }
}
