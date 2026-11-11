package com.example.lagallens.data.repository

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.datasource.local.ProfileAvatarStore
import com.example.lagallens.data.datasource.local.datastore.ProfileEdits
import com.example.lagallens.data.datasource.local.datastore.ProfileLocalEdits
import com.example.lagallens.data.datasource.remote.UserRemoteDataSource
import com.example.lagallens.data.datasource.remote.api.UserApi
import com.example.lagallens.data.model.response.ApiEnvelope
import com.example.lagallens.data.model.response.UserResponse
import com.example.lagallens.domain.model.UserProfile
import com.example.lagallens.domain.repository.ProfileAvatarUpdateException
import com.example.lagallens.domain.repository.ProfilePhoneRejectedException
import com.google.gson.JsonParser
import kotlinx.coroutines.test.runTest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.InputStream

class RemoteProfileRepositoryTest {
    private val session = RemoteRepositorySession()
    private val api = UserFixture()
    private val avatars = AvatarFixture()
    private val device = DeviceEditsFixture()
    private val repository = RemoteProfileRepository(session, UserRemoteDataSource(lazyOf(api)), avatars, device)

    @Test fun phoneAndAvatarComeFromTheServerAndTheImageIsDownloadedOnce() = runTest {
        api.user = api.user.copy(phone = "0901234567", avatar = AVATAR_PATH)

        val first = repository.getProfile()
        val second = repository.getProfile()

        assertEquals(UserProfile("Original Name", "tester@example.test", "0901234567", "file://avatar/42/v1"), first)
        assertEquals(first, second)
        assertEquals(1, api.avatarReads)
    }

    @Test fun accountWithoutAvatarHasNoPhotoAndClearsAnOldCache() = runTest {
        avatars.files["42"] = "old" to "file://avatar/42/old"

        assertNull(repository.getProfile().photoUri)
        assertTrue("42" !in avatars.files)
        assertEquals(0, api.avatarReads)
    }

    @Test fun savingDetailsPatchesTheCurrentUserWithTheTrimmedPhone() = runTest {
        repository.getProfile()

        val saved = repository.saveProfile(" Updated Name ", " 0901234567 ", null)

        assertEquals(listOf("""{"name":"Updated Name","phone":"0901234567"}"""), api.patches)
        assertEquals("0901234567", saved.phone)
        assertEquals(1, api.reads)
    }

    @Test fun clearingThePhoneSendsAnExplicitNull() = runTest {
        api.user = api.user.copy(phone = "0901234567")
        repository.getProfile()

        val saved = repository.saveProfile(api.user.name, "  ", null)

        assertEquals(listOf("""{"name":"Original Name","phone":null}"""), api.patches)
        assertEquals("", saved.phone)
    }

    @Test fun unchangedDetailsSendNoRequest() = runTest {
        api.user = api.user.copy(phone = "0901234567")
        repository.getProfile()

        repository.saveProfile(api.user.name, "0901234567", null)

        assertTrue(api.patches.isEmpty())
        assertTrue(api.uploads.isEmpty())
    }

    @Test fun aRejectedPhoneIsReportedAsSuch() = runTest {
        repository.getProfile()
        api.patchFailure = httpError(400)

        val failure = runCatching { repository.saveProfile(api.user.name, "12345", null) }.exceptionOrNull()

        assertTrue(failure is ProfilePhoneRejectedException)
    }

    @Test fun aPickedPhotoIsUploadedOnceAndCachedFromTheUploadedBytes() = runTest {
        repository.getProfile()

        val saved = repository.saveProfile(api.user.name, "", PICKED)
        val again = repository.saveProfile(api.user.name, "", saved.photoUri)

        assertEquals(listOf(PICKED), avatars.encoded)
        assertEquals(1, api.uploads.size)
        assertEquals("file://avatar/42/v2", saved.photoUri)
        assertEquals(saved, again)
        assertEquals(0, api.avatarReads)
    }

    @Test fun aFailedAvatarUploadReportsTheSavedDetails() = runTest {
        repository.getProfile()
        val serverError = httpError(500)
        api.uploadFailure = serverError

        val failure = runCatching {
            repository.saveProfile("Updated Name", "0901234567", PICKED)
        }.exceptionOrNull() as ProfileAvatarUpdateException

        assertSame(serverError, failure.cause)
        assertEquals(UserProfile("Updated Name", api.user.email, "0901234567", null), failure.savedProfile)
        assertEquals(1, api.patches.size)
    }

    @Test fun deviceEditsFromOlderBuildsMoveToTheServerOnce() = runTest {
        device.values["42"] = ProfileEdits("0901234567", PICKED)

        val profile = repository.getProfile()
        repository.getProfile()

        assertEquals("0901234567", profile.phone)
        assertEquals("file://avatar/42/v3", profile.photoUri)
        assertEquals(1, api.patches.size)
        assertEquals(1, api.uploads.size)
        assertTrue("42" !in device.values)
    }

    @Test fun deviceEditsAreKeptWhenMovingThemFails() = runTest {
        device.values["42"] = ProfileEdits("0901234567", null)
        api.patchFailure = httpError(503)

        val profile = repository.getProfile()

        assertEquals("", profile.phone)
        assertEquals(ProfileEdits("0901234567", null), device.values["42"])
    }

    @Test fun anInvalidDevicePhoneIsDroppedInsteadOfRetriedForever() = runTest {
        device.values["42"] = ProfileEdits("123", null)
        api.patchFailure = httpError(400)

        repository.getProfile()

        assertTrue("42" !in device.values)
    }

    private fun httpError(code: Int) = HttpException(Response.error<Unit>(code, "".toResponseBody()))

    private inner class UserFixture : UserApi {
        var user = UserResponse(42, "Original Name", "tester@example.test", null, null, true, listOf("USER"), "", "v1")
        var reads = 0
        var avatarReads = 0
        val patches = mutableListOf<String>()
        val uploads = mutableListOf<ByteArray>()
        var patchFailure: Exception? = null
        var uploadFailure: Exception? = null

        override suspend fun me(scope: AuthSessionScope): ApiEnvelope<UserResponse> {
            assertEquals("session", scope.key)
            reads++
            return ApiEnvelope(true, user)
        }

        override suspend fun updateMe(scope: AuthSessionScope, body: RequestBody): ApiEnvelope<UserResponse> {
            assertEquals("session", scope.key)
            patchFailure?.let { throw it }
            val json = Buffer().also(body::writeTo).readUtf8()
            patches.add(json)
            val fields = JsonParser.parseString(json).asJsonObject
            val phone = fields.get("phone").takeUnless { it.isJsonNull }?.asString
            user = user.copy(name = fields.get("name").asString, phone = phone, updatedAt = "v${patches.size + 1}")
            return ApiEnvelope(true, user)
        }

        override suspend fun uploadAvatar(scope: AuthSessionScope, file: MultipartBody.Part): ApiEnvelope<UserResponse> {
            assertEquals("session", scope.key)
            uploadFailure?.let { throw it }
            uploads.add(Buffer().also(file.body::writeTo).readByteArray())
            user = user.copy(avatar = AVATAR_PATH, updatedAt = "v${patches.size + uploads.size + 1}")
            return ApiEnvelope(true, user)
        }

        override suspend fun avatar(scope: AuthSessionScope): ResponseBody {
            avatarReads++
            return "image".toResponseBody()
        }
    }

    private class AvatarFixture : ProfileAvatarStore {
        val files = mutableMapOf<String, Pair<String, String>>()
        val encoded = mutableListOf<String>()

        override fun encodeForUpload(uri: String): ByteArray {
            encoded.add(uri)
            return byteArrayOf(1, 2, 3)
        }

        override fun cached(userId: String, version: String) =
            files[userId]?.takeIf { it.first == version }?.second

        override fun save(userId: String, version: String, input: InputStream): String {
            input.readBytes()
            return "file://avatar/$userId/$version".also { files[userId] = version to it }
        }

        override fun clear(userId: String) {
            files.remove(userId)
        }
    }

    private class DeviceEditsFixture : ProfileLocalEdits {
        val values = mutableMapOf<String, ProfileEdits>()

        override fun readEdits(userId: String) = values[userId] ?: ProfileEdits("", null)
        override fun clearEdits(userId: String) {
            values.remove(userId)
        }
    }

    private companion object {
        const val AVATAR_PATH = "/users/me/avatar"
        const val PICKED = "content://media/picked/1"
    }
}
