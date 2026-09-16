package com.example.lagallens.domain.model.camera

sealed interface DocumentDetection {
    data class Ready(
        val areaRatio: Double,
        val centerX: Double,
        val centerY: Double
    ) : DocumentDetection

    data class Partial(val areaRatio: Double) : DocumentDetection
    data object Unstable : DocumentDetection
    data object NotFound : DocumentDetection
    data object Unavailable : DocumentDetection
}
