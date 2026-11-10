package com.example.lagallens.data.repository

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.datasource.local.datastore.ProfileEdits
import com.example.lagallens.data.datasource.local.datastore.ProfileLocalEdits
import com.example.lagallens.data.datasource.remote.UserRemoteDataSource
import com.example.lagallens.data.datasource.remote.api.UserApi
import com.example.lagallens.data.mapper.toProfile
import com.example.lagallens.data.model.request.UpdateUserRequest
import com.example.lagallens.data.model.response.ApiEnvelope
import com.example.lagallens.data.model.response.UserResponse
import com.example.lagallens.domain.model.UserProfile
import com.example.lagallens.domain.repository.ProfileNameUpdateException
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class RemoteProfileRepositoryTest {
    private val user = UserResponse(42, "Original Name", "tester@example.test", true, listOf("USER"), "", "")
    private val session = RemoteRepositorySession()
    private val api = UserFixture()
    private val local = LocalFixture()
    private val repository = RemoteProfileRepository(session, UserRemoteDataSource(lazyOf(api)), local)

    @Test fun mapperUsesServerIdentityAndLocalEdits() {
        assertEquals(
            UserProfile("Original Name", "tester@example.test", "123456789", "content://profile/photo"),
            user.toProfile("123456789", "content://profile/photo")
        )
    }

    @Test fun changingNamePatchesCurrentUserAndSavesLocalFields() = runTest {
        repository.getProfile()
        val updated = repository.saveProfile(" Updated Name ", " 123456789 ", "content://profile/photo")

        assertEquals(1, api.reads)
        assertEquals(listOf(UpdateUserRequest("Updated Name")), api.updates)
        assertEquals(UserProfile("Updated Name", user.email, "123456789", "content://profile/photo"), updated)
        assertEquals(ProfileEdits("123456789", "content://profile/photo"), local.values["42"])
    }

    @Test fun changingOnlyLocalFieldsSendsNoRequestAfterProfileLoads() = runTest {
        repository.getProfile()
        val updated = repository.saveProfile(user.name, "123456789", null)

        assertEquals(1, api.reads)
        assertTrue(api.updates.isEmpty())
        assertEquals("123456789", updated.phone)
    }

    @Test fun failedPatchReportsTheOldNameAndKeepsLocalFields() = runTest {
        repository.getProfile()
        val conflict = HttpException(Response.error<Unit>(409, "".toResponseBody()))
        val edits = ProfileEdits("123456789", "content://profile/photo")
        api.beforeUpdate = {
            assertEquals(edits, local.values["42"])
            throw conflict
        }

        val failure = runCatching {
            repository.saveProfile("Updated Name", edits.phone, edits.photoUri)
        }.exceptionOrNull() as ProfileNameUpdateException

        val expected = UserProfile(user.name, user.email, edits.phone, edits.photoUri)
        assertSame(conflict, failure.cause)
        assertEquals(expected, failure.savedProfile)
        assertEquals(edits, local.values["42"])
        assertEquals(expected, repository.getProfile())
    }

    @Test fun clearsOnlyCurrentUsersLocalFields() = runTest {
        local.saveEdits("42", "111111111", null)
        val other = local.saveEdits("43", "222222222", null)

        assertTrue(repository.clearSessionData())
        assertNull(local.values["42"])
        assertEquals(other, local.values["43"])
        assertEquals(0, api.reads)
    }

    private inner class UserFixture : UserApi {
        var reads = 0
        val updates = mutableListOf<UpdateUserRequest>()
        var beforeUpdate: () -> Unit = {}

        override suspend fun me(scope: AuthSessionScope): ApiEnvelope<UserResponse> {
            assertEquals("session", scope.key)
            reads++
            return ApiEnvelope(true, user)
        }

        override suspend fun update(scope: AuthSessionScope, id: String, request: UpdateUserRequest): ApiEnvelope<UserResponse> {
            assertEquals("session", scope.key)
            assertEquals("42", id)
            beforeUpdate()
            updates.add(request)
            return ApiEnvelope(true, user.copy(name = request.name))
        }
    }

    private class LocalFixture : ProfileLocalEdits {
        val values = mutableMapOf<String, ProfileEdits>()

        override fun readEdits(userId: String) = values[userId] ?: ProfileEdits("", null)
        override fun saveEdits(userId: String, phone: String, photoUri: String?) =
            ProfileEdits(phone, photoUri).also { values[userId] = it }

        override fun clearEdits(userId: String): Boolean {
            values.remove(userId)
            return true
        }
    }
}
