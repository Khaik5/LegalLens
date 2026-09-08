package com.example.lagallens.presentation.feature.camera.capture.contract

import androidx.annotation.StringRes
import com.example.lagallens.presentation.feature.camera.core.DocumentQualityResult
import com.example.lagallens.presentation.feature.camera.core.ScanPage

sealed interface CameraCaptureEffect {
    data object NavigateBack : CameraCaptureEffect
    data object CapturePhoto : CameraCaptureEffect
    data object OpenGallery : CameraCaptureEffect
    data class ShowQualityResult(
        val page: ScanPage,
        val qualityResult: DocumentQualityResult
    ) : CameraCaptureEffect
    data class NavigateToPageManager(val pages: List<ScanPage>) : CameraCaptureEffect
    data class ShowMessage(@StringRes val messageRes: Int) : CameraCaptureEffect
}
