package com.example.lagallens

import androidx.test.platform.app.InstrumentationRegistry
import com.example.lagallens.data.datasource.local.datastore.AuthLocalDataSource
import com.example.lagallens.data.repository.LocalAuthRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthDemoSessionTest {
    @Test fun loginPersistsSessionAndLogoutClearsIt() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences("auth_demo_session", android.content.Context.MODE_PRIVATE)
        val previous = preferences.all.toMap()
        val repository = LocalAuthRepository(AuthLocalDataSource(context))
        try {
            preferences.edit().clear().commit()
            repository.logout()
            assertFalse(repository.authenticate("", ""))
            assertTrue(repository.authenticate("demo.user@example.test", "valid-demo-password"))
            assertEquals("demo.user@example.test", LocalAuthRepository(AuthLocalDataSource(context)).currentUserEmail())
            assertTrue(repository.logout())
            assertNull(repository.currentUserEmail())
        } finally {
            preferences.edit().clear().commit()
            val editor = preferences.edit()
            previous.forEach { (key, value) -> when (value) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
            } }
            editor.commit()
        }
    }

    @Test fun changedDemoPasswordIsHashedAndVerifiedAfterLogout() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences("auth_demo_session", android.content.Context.MODE_PRIVATE)
        val previous = preferences.all.toMap()
        val repository = LocalAuthRepository(AuthLocalDataSource(context))
        try {
            preferences.edit().clear().commit()
            assertTrue(repository.authenticate("password.test@example.test", "Old-demo-123"))
            assertFalse(repository.changePassword("wrong", "New-demo-456"))
            assertTrue(repository.changePassword("Old-demo-123", "New-demo-456"))
            repository.logout()
            assertFalse(repository.authenticate("password.test@example.test", "Old-demo-123"))
            assertTrue(repository.authenticate("password.test@example.test", "New-demo-456"))
            assertFalse(preferences.all.values.filterIsInstance<String>().any { "Old-demo-123" in it || "New-demo-456" in it })
        } finally {
            preferences.edit().clear().commit()
            val editor = preferences.edit()
            previous.forEach { (key, value) -> when (value) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
            } }
            editor.commit()
        }
    }
}
