package com.example.lagallens.data.datasource.local.datastore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DemoAccountKeysTest {
    @Test fun lookupNormalizesEmailAndSeparatesAccounts() {
        assertEquals(DemoAccountKeys.lookup(" User@Example.test "), DemoAccountKeys.lookup("user@example.test"))
        assertNotEquals(DemoAccountKeys.lookup("user@example.test"), DemoAccountKeys.lookup("other@example.test"))
    }

    @Test fun blankIdentityIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { DemoAccountKeys.lookup("  ") }
    }
}
