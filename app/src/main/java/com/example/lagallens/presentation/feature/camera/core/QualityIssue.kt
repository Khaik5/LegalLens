package com.example.lagallens.presentation.feature.camera.core

import androidx.annotation.StringRes
import com.example.lagallens.R

enum class QualityIssue(@StringRes val messageRes: Int) {
    BLUR(R.string.camera_quality_blur),
    EXPOSURE(R.string.camera_quality_exposure),
    SHAKE(R.string.camera_quality_shake),
    DOCUMENT_SMALL(R.string.camera_quality_document_small),
    CROP_RISK(R.string.camera_quality_crop_risk),
    DOCUMENT_NOT_FOUND(R.string.camera_quality_document_not_found)
}
