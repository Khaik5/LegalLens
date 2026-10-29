package com.example.lagallens.data.datasource.local.datastore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AccountStorageNameTest {
    @Test fun accountNamespacesAreStableAndSeparateFromLegacy() {
        val a = "b021a90c-3fb1-49cb-a908-02028c27aa11"
        val b = "b021a90c-3fb1-49cb-a908-02028c27aa12"
        assertEquals(AccountStorageName.forUser("draft", a), AccountStorageName.forUser("draft", a.uppercase()))
        assertNotEquals(AccountStorageName.forUser("draft", a), AccountStorageName.forUser("draft", b))
        assertNotEquals("draft", AccountStorageName.forUser("draft", a))
    }

    @Test fun missingOrInvalidAccountCannotAccessStorage() {
        assertThrows(IllegalStateException::class.java) { AccountStorageName.forUser("draft", null) }
        assertThrows(IllegalStateException::class.java) { AccountStorageName.forUser("draft", "bad-id") }
    }
}
