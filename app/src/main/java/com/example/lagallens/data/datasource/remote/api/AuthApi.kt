package com.example.lagallens.data.datasource.remote.api

import com.example.lagallens.data.model.request.ChallengeRequest
import com.example.lagallens.data.model.request.LoginRequest
import com.example.lagallens.data.model.request.RegisterRequest
import com.example.lagallens.data.model.request.RefreshTokenRequest
import com.example.lagallens.data.model.request.ResetPasswordRequest
import com.example.lagallens.data.model.request.VerifyChallengeRequest
import com.example.lagallens.data.model.response.ApiEnvelope
import com.example.lagallens.data.model.response.ChallengeResponse
import com.example.lagallens.data.model.response.LoginResponse
import com.example.lagallens.data.model.response.RegisterResponse
import com.example.lagallens.data.model.response.RefreshResponse
import retrofit2.Call
import com.example.lagallens.data.model.response.VerifyChallengeResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("authentication/login")
    suspend fun login(@Body request: LoginRequest): ApiEnvelope<LoginResponse>

    @POST("authentication/register")
    suspend fun register(@Body request: RegisterRequest): ApiEnvelope<RegisterResponse>

    @POST("authentication/challenges")
    suspend fun requestChallenge(@Body request: ChallengeRequest): ApiEnvelope<ChallengeResponse>

    @POST("authentication/challenges/verify")
    suspend fun verifyChallenge(@Body request: VerifyChallengeRequest): ApiEnvelope<VerifyChallengeResponse>

    @POST("authentication/refresh")
    fun refresh(@Body request: RefreshTokenRequest): Call<ApiEnvelope<RefreshResponse>>

    @POST("authentication/password/reset")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<Unit>
}
