package com.example.lagallens.core.network

import android.util.Log
import okhttp3.Interceptor
import java.io.IOException

/**
 * Debug builds only: logs method, path and result of each call so a failing screen can be traced to its request.
 * It never logs headers, query strings or bodies, which can carry tokens and contract text.
 */
class HttpStatusLogInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val request = chain.request()
        val call = "${request.method} ${request.url.encodedPath}"
        return try {
            chain.proceed(request).also { Log.d(TAG, "$call -> ${it.code}") }
        } catch (error: IOException) {
            Log.d(TAG, "$call -> ${error.javaClass.simpleName}")
            throw error
        }
    }

    private companion object {
        const val TAG = "LegalLensHttp"
    }
}
