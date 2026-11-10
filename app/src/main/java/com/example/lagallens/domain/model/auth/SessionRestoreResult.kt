package com.example.lagallens.domain.model.auth

sealed interface SessionRestoreResult {
    data object Restored : SessionRestoreResult
    data object Expired : SessionRestoreResult
    data object NoSession : SessionRestoreResult
    data class Failed(val cause: Throwable) : SessionRestoreResult
}
