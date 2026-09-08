package com.example.lagallens.presentation.feature.camera.capture.contract

import com.example.lagallens.presentation.feature.camera.core.DocumentDetection
import com.example.lagallens.presentation.feature.camera.core.ScanPage

data class CameraCaptureUiState(
    val isFlashEnabled: Boolean = false,
    val isAutoCaptureEnabled: Boolean = true,
    val detection: DocumentDetection = DocumentDetection.NotFound,
    val pages: List<ScanPage> = emptyList(),
    val isCapturing: Boolean = false
)
