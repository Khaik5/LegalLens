package com.example.lagallens.domain.repository

interface OnboardingRepository {
    suspend fun isCompleted(): Boolean

    suspend fun markCompleted()
}
