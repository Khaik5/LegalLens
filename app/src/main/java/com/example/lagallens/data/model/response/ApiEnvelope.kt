package com.example.lagallens.data.model.response

import com.example.lagallens.core.network.ApiFailureMapper

data class ApiEnvelope<T : Any>(val success: Boolean, val data: T?) {
    fun requireData(feature: String): T =
        ApiFailureMapper.requireBody(data.takeIf { success }, feature)
}
