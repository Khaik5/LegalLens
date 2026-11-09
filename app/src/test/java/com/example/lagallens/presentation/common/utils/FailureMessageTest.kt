package com.example.lagallens.presentation.common.utils

import com.example.lagallens.R
import com.example.lagallens.core.network.BackendNotConfiguredException
import com.example.lagallens.core.network.EmptyApiResponseException
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import org.junit.Assert.*
import org.junit.Test

class FailureMessageTest {
    @Test fun failuresAreClassifiedWithoutLeakingServerDetails() {
        assertEquals(R.string.operation_network_error, IOException().toFailureMessage())
        assertEquals(R.string.operation_network_error, SocketTimeoutException().toFailureMessage())
        assertEquals(R.string.operation_session_error, HttpException(Response.error<Any>(401, "private".toResponseBody())).toFailureMessage())
        assertEquals(R.string.operation_data_error, EmptyApiResponseException("data").toFailureMessage())
        assertEquals(R.string.auth_backend_not_configured, BackendNotConfiguredException("auth").toFailureMessage())
        assertEquals(R.string.operation_failed, IllegalStateException("disk").toFailureMessage())
    }
    @Test(expected = CancellationException::class) fun cancellationIsNeverAUserError() {
        CancellationException().toFailureMessage()
    }
}
