package com.example.lagallens.core.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailValidationTest {
    @Test fun acceptsCommonAddress() = assertTrue(isValidEmail("user@example.com"))
    @Test fun acceptsPlusAddress() = assertTrue(isValidEmail("user+tag@example.com"))
    @Test fun acceptsApostrophe() = assertTrue(isValidEmail("o'connor@example.com"))
    @Test fun rejectsMissingDomainSuffix() = assertFalse(isValidEmail("a@b"))
    @Test fun rejectsMissingAtSign() = assertFalse(isValidEmail("user.example.com"))
    @Test fun rejectsWhitespace() {
        listOf("user name@example.com", "user@example .com", "user\n@example.com", " user@example.com").forEach {
            assertFalse(it, isValidEmail(it))
        }
    }
}
