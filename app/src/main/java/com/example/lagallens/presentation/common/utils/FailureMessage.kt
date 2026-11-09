package com.example.lagallens.presentation.common.utils

import com.example.lagallens.R
import com.example.lagallens.core.network.ApiFailureKind
import com.example.lagallens.core.network.ApiFailureMapper
import kotlinx.coroutines.CancellationException

fun Throwable.toFailureMessage(): Int {
    if (this is CancellationException) throw this
    return when (ApiFailureMapper.map(this).kind) {
        ApiFailureKind.BACKEND_NOT_CONFIGURED -> R.string.auth_backend_not_configured
        ApiFailureKind.NETWORK, ApiFailureKind.TIMEOUT -> R.string.operation_network_error
        ApiFailureKind.UNAUTHORIZED -> R.string.operation_session_error
        ApiFailureKind.FORBIDDEN -> R.string.operation_access_error
        ApiFailureKind.NOT_FOUND, ApiFailureKind.EMPTY_RESPONSE, ApiFailureKind.VALIDATION -> R.string.operation_data_error
        ApiFailureKind.RATE_LIMITED -> R.string.operation_rate_error
        ApiFailureKind.SERVER -> R.string.operation_server_error
        else -> R.string.operation_failed
    }
}
