package com.example.lagallens.presentation.feature.camera.pages.contract

import com.example.lagallens.domain.model.camera.ScanPage

sealed interface CameraPageManagerUiEffect {
    data class ReturnToCapture(
        val pages: List<ScanPage>,
        val deletedPaths: List<String> = emptyList()
    ) : CameraPageManagerUiEffect
    data class NavigateToOcrProcessing(val pages: List<ScanPage>) : CameraPageManagerUiEffect
}
