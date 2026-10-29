package com.example.lagallens.core.network

import android.content.Context
import com.example.lagallens.BuildConfig
import com.example.lagallens.data.datasource.local.datastore.EncryptedTokenStore
import com.example.lagallens.di.AppDataMode
import com.example.lagallens.di.DataModeConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class AuthHeaderInterceptor(private val tokenStore: EncryptedTokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val token = tokenStore.accessToken().takeIf { DataModeConfig.current == AppDataMode.REAL }
        val request = chain.request().newBuilder().apply {
            if (!token.isNullOrBlank()) header("Authorization", "Bearer $token")
        }.build()
        val response = chain.proceed(request)
        if (response.code == 401 && !token.isNullOrBlank()) {
            tokenStore.clearAccessTokenIfMatches(token)
        }
        return response
    }
}

object NetworkModule {
    fun provideOkHttpClient(context: Context): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(AuthHeaderInterceptor(EncryptedTokenStore(context)))
        // Contract text and credentials are sensitive; request/response body logging is intentionally disabled.
        .build()

    fun provideRetrofit(context: Context): Retrofit {
        val baseUrl = configuredHttpsBaseUrl()
            ?: throw BackendNotConfiguredException("Network (set an approved HTTPS -PlegalLensApiBaseUrl before enabling REAL API calls)")
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(provideOkHttpClient(context))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    internal fun configuredHttpsBaseUrl(): HttpUrl? {
        if (DataModeConfig.current != AppDataMode.REAL) return null
        val url = BuildConfig.API_BASE_URL.trim().toHttpUrlOrNull() ?: return null
        return url.takeIf { it.isHttps && it.encodedPath.endsWith('/') }
    }
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
