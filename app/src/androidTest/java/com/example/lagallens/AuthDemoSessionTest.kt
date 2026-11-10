package com.example.lagallens

import androidx.test.platform.app.InstrumentationRegistry
import com.example.lagallens.data.datasource.local.datastore.AuthLocalDataSource
import com.example.lagallens.data.repository.LocalAuthRepository
import com.example.lagallens.domain.model.auth.AuthFailure
import com.example.lagallens.domain.model.auth.AuthResult
import com.example.lagallens.domain.model.auth.ChangePasswordResult
import com.example.lagallens.domain.model.auth.OtpVerification
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
            val registration = repository.register("New User", email, "Chosen-123") as AuthResult.Success
            val challenge = checkNotNull(registration.value)
            assertNull(repository.currentSessionKey())
            assertEquals(AuthResult.Failed(AuthFailure.INVALID_CODE), repository.verifyOtp(challenge.challengeId, "12a456", OtpPurpose.REGISTRATION))
            assertNull(repository.currentSessionKey())
            assertEquals(AuthResult.Success(OtpVerification.Verified), repository.verifyOtp(challenge.challengeId, "123456", OtpPurpose.REGISTRATION))
            assertEquals(email, repository.currentUserEmail())
            assertEquals("New User", createdName)
            assertEquals(AuthResult.Failed(AuthFailure.EMAIL_TAKEN), repository.register("Other", email, "Other-123"))
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
            val challenge = (repository.requestPasswordReset(email) as AuthResult.Success).value
            val challengeId = challenge.challengeId
            assertEquals(AuthResult.Failed(AuthFailure.REJECTED), repository.confirmPasswordReset(challengeId, challengeId, "New-demo-456"))
            val verification = repository.verifyOtp(challengeId, "123456", OtpPurpose.PASSWORD_RESET) as AuthResult.Success
            val proof = (verification.value as OtpVerification.PasswordResetVerified).verificationToken
            assertNull(repository.currentSessionKey())
            assertEquals(AuthResult.Success(Unit), repository.confirmPasswordReset(challengeId, proof, "New-demo-456"))
            assertEquals(AuthResult.Failed(AuthFailure.REJECTED), repository.confirmPasswordReset(challengeId, proof, "Again-demo-789"))
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
            assertEquals(ChangePasswordResult.IncorrectCurrentPassword, repository.changePassword("wrong", "New-demo-456"))
            assertEquals(ChangePasswordResult.Updated, repository.changePassword("Old-demo-123", "New-demo-456"))
            repository.logout()
            assertFalse(repository.authenticate("password.test@example.test", "Old-demo-123"))
            assertTrue(repository.authenticate("password.test@example.test", "New-demo-456"))
            assertFalse(preferences.all.values.filterIsInstance<String>().any { "Old-demo-123" in it || "New-demo-456" in it })
        } finally {
            context.deleteSharedPreferences(name)
        }
    }
}
