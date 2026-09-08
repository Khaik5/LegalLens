package com.example.lagallens.presentation.feature.camera.pages.contract

import com.example.lagallens.presentation.feature.camera.core.ScanPage

sealed interface CameraPageManagerEffect {
    data class ReturnToCapture(
        val pages: List<ScanPage>,
        val deletedPaths: List<String> = emptyList()
    ) : CameraPageManagerEffect
    data class NavigateToOcrProcessing(val pages: List<ScanPage>) : CameraPageManagerEffect
}
