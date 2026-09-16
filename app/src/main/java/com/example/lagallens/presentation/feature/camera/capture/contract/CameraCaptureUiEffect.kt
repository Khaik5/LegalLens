package com.example.lagallens.presentation.feature.camera.capture.contract

import androidx.annotation.StringRes
import com.example.lagallens.domain.model.camera.DocumentQualityResult
import com.example.lagallens.domain.model.camera.ScanPage

sealed interface CameraCaptureUiEffect {
    data object NavigateBack : CameraCaptureUiEffect
    data object CapturePhoto : CameraCaptureUiEffect
    data object OpenGallery : CameraCaptureUiEffect
    data class ShowQualityResult(
        val page: ScanPage,
        val qualityResult: DocumentQualityResult
    ) : CameraCaptureUiEffect
    data class NavigateToPageManager(val pages: List<ScanPage>) : CameraCaptureUiEffect
    data class ShowMessage(@param:StringRes val messageRes: Int) : CameraCaptureUiEffect
}
