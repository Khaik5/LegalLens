package com.example.lagallens.presentation.feature.camera.capture.contract

import com.example.lagallens.domain.model.camera.DocumentDetection
import com.example.lagallens.domain.model.camera.ScanPage

data class CameraCaptureUiState(
    val isFlashEnabled: Boolean = false,
    val isAutoCaptureEnabled: Boolean = true,
    val detection: DocumentDetection = DocumentDetection.NotFound,
    val pages: List<ScanPage> = emptyList(),
    val isCapturing: Boolean = false,
    val pendingQualityChecks: List<PendingQualityCheck> = emptyList()
)

/** A captured page waiting for the user's decision; kept in state so a recreated screen shows it again. */
data class PendingQualityCheck(
    val page: com.example.lagallens.domain.model.camera.ScanPage,
    val qualityResult: com.example.lagallens.domain.model.camera.DocumentQualityResult
)
