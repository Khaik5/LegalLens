package com.example.lagallens

import androidx.test.platform.app.InstrumentationRegistry
import com.example.lagallens.data.datasource.local.datastore.AuthLocalDataSource
import com.example.lagallens.data.repository.LocalAuthRepository
import com.example.lagallens.domain.model.auth.AuthResult
import com.example.lagallens.domain.model.auth.OtpPurpose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthDemoSessionTest {
    @Test fun loginPersistsSessionAndLogoutClearsIt() = kotlinx.coroutines.runBlocking<Unit> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "test_auth_${java.util.UUID.randomUUID()}"
        val preferences = context.getSharedPreferences(name, android.content.Context.MODE_PRIVATE)
        val repository = LocalAuthRepository(AuthLocalDataSource(context, name))
        try {
            repository.logout()
            assertFalse(repository.authenticate("", ""))
            assertTrue(repository.authenticate("demo.user@example.test", "valid-demo-password"))
            assertEquals("demo.user@example.test", LocalAuthRepository(AuthLocalDataSource(context, name)).currentUserEmail())
            assertTrue(repository.logout())
            assertNull(repository.currentUserEmail())
        } finally {
            context.deleteSharedPreferences(name)
        }
    }

    @Test fun registrationCreatesTheAccountOnlyAfterOtpAndKeepsThePassword() = kotlinx.coroutines.runBlocking<Unit> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "test_auth_${java.util.UUID.randomUUID()}"
        var createdName: String? = null
        val repository = LocalAuthRepository(AuthLocalDataSource(context, name)) { createdName = it }
        try {
            val email = "new.user@example.test"
            assertEquals(AuthResult.SUCCESS, repository.register("New User", email, "Chosen-123"))
            assertNull(repository.currentSessionKey())
            assertEquals(AuthResult.INVALID_CODE, repository.verifyOtp(email, "12a456", OtpPurpose.REGISTRATION))
            assertNull(repository.currentSessionKey())
            assertEquals(AuthResult.SUCCESS, repository.verifyOtp(email, "123456", OtpPurpose.REGISTRATION))
            assertEquals(email, repository.currentUserEmail())
            assertEquals("New User", createdName)
            assertEquals(AuthResult.EMAIL_TAKEN, repository.register("Other", email, "Other-123"))
            repository.logout()
            assertFalse(repository.authenticate(email, "Other-123"))
            assertTrue(repository.authenticate(email, "Chosen-123"))
        } finally {
            context.deleteSharedPreferences(name)
        }
    }

    @Test fun passwordResetRequiresVerifiedOtpAndCreatesNoSession() = kotlinx.coroutines.runBlocking<Unit> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "test_auth_${java.util.UUID.randomUUID()}"
        val repository = LocalAuthRepository(AuthLocalDataSource(context, name))
        try {
            val email = "reset.user@example.test"
            assertTrue(repository.authenticate(email, "Old-demo-123"))
            repository.logout()
            assertEquals(AuthResult.REJECTED, repository.confirmPasswordReset(email, "New-demo-456"))
            assertEquals(AuthResult.SUCCESS, repository.verifyOtp(email, "123456", OtpPurpose.PASSWORD_RESET))
            assertNull(repository.currentSessionKey())
            assertEquals(AuthResult.SUCCESS, repository.confirmPasswordReset(email, "New-demo-456"))
            assertEquals(AuthResult.REJECTED, repository.confirmPasswordReset(email, "Again-demo-789"))
            assertFalse(repository.authenticate(email, "Old-demo-123"))
            assertTrue(repository.authenticate(email, "New-demo-456"))
        } finally {
            context.deleteSharedPreferences(name)
        }
    }

    @Test fun changedDemoPasswordIsHashedAndVerifiedAfterLogout() = kotlinx.coroutines.runBlocking<Unit> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "test_auth_${java.util.UUID.randomUUID()}"
        val preferences = context.getSharedPreferences(name, android.content.Context.MODE_PRIVATE)
        val repository = LocalAuthRepository(AuthLocalDataSource(context, name))
        try {
            assertTrue(repository.authenticate("password.test@example.test", "Old-demo-123"))
            assertFalse(repository.changePassword("wrong", "New-demo-456"))
            assertTrue(repository.changePassword("Old-demo-123", "New-demo-456"))
            repository.logout()
            assertFalse(repository.authenticate("password.test@example.test", "Old-demo-123"))
            assertTrue(repository.authenticate("password.test@example.test", "New-demo-456"))
            assertFalse(preferences.all.values.filterIsInstance<String>().any { "Old-demo-123" in it || "New-demo-456" in it })
        } finally {
            context.deleteSharedPreferences(name)
        }
    }
}
