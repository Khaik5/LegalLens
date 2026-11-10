package com.example.lagallens.core.network

import com.example.lagallens.BuildConfig
import com.example.lagallens.data.datasource.local.datastore.AuthSessionStore
import com.example.lagallens.di.AppDataMode
import com.example.lagallens.di.DataModeConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class AuthHeaderInterceptor(private val tokens: AuthSessionStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val original = chain.request()
        val requestedScope = original.tag(AuthSessionScope::class.java)
        val current = tokens.accessSession()
        if (requestedScope != null && requestedScope.key != current?.key) {
            throw IOException("Session changed before sending request")
        }
        if (current == null || original.header("Authorization") != null) return chain.proceed(original)
        val request = original.newBuilder()
            .header("Authorization", "Bearer ${current.token}")
            .tag(AuthSessionScope::class.java, requestedScope ?: AuthSessionScope(current.key))
            .build()
        return chain.proceed(request)
    }
}

object NetworkModule {
    private fun clientBuilder(): OkHttpClient.Builder = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .followRedirects(false)
        // Contract text and credentials are sensitive; request/response body logging is intentionally disabled.

    fun provideRetrofit(): Retrofit = retrofit(clientBuilder().retryOnConnectionFailure(false).build())

    fun provideAuthenticatedRetrofit(tokens: AuthSessionStore, refresh: (String) -> TokenRefreshResult): Retrofit =
        retrofit(
            clientBuilder()
                .addInterceptor(AuthHeaderInterceptor(tokens))
                .authenticator(TokenRefreshAuthenticator(tokens, refresh))
                .build()
        )

    private fun retrofit(client: OkHttpClient): Retrofit {
        val baseUrl = configuredBaseUrl()
            ?: throw BackendNotConfiguredException("Network (set an HTTPS API base URL or use the local debug build)")
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    internal fun configuredBaseUrl(): HttpUrl? {
        if (DataModeConfig.current != AppDataMode.REAL) return null
        val url = BuildConfig.API_BASE_URL.trim().toHttpUrlOrNull() ?: return null
        if (url.isHttps) return url.takeIf { it.encodedPath.endsWith('/') }
        return url.takeIf {
            BuildConfig.BUILD_TYPE == "debug" && it.host in LOCAL_DEBUG_HOSTS &&
                it.port == 3000 && it.encodedPath == "/" && it.query == null &&
                it.fragment == null && it.username.isEmpty() && it.password.isEmpty()
        }
    }

    /** Emulator host alias, and the device loopback used with `adb reverse tcp:3000 tcp:3000`. */
    private val LOCAL_DEBUG_HOSTS = setOf("10.0.2.2", "127.0.0.1")
}

enum class HttpFailureKind { UNAUTHORIZED, FORBIDDEN, NOT_FOUND, CONFLICT, VALIDATION, RATE_LIMITED, SERVER, TIMEOUT, REQUEST_TIMEOUT, OTHER }

fun httpFailureKind(statusCode: Int): HttpFailureKind = when (statusCode) {
    401 -> HttpFailureKind.UNAUTHORIZED
    403 -> HttpFailureKind.FORBIDDEN
    404 -> HttpFailureKind.NOT_FOUND
    409 -> HttpFailureKind.CONFLICT
    400, 422 -> HttpFailureKind.VALIDATION
    429 -> HttpFailureKind.RATE_LIMITED
    408 -> HttpFailureKind.REQUEST_TIMEOUT
    in 500..599 -> HttpFailureKind.SERVER
    else -> HttpFailureKind.OTHER
}

fun Throwable.isNetworkTimeout(): Boolean = this is SocketTimeoutException
