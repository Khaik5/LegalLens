package com.example.lagallens.presentation.common.model

import androidx.annotation.ColorRes
import androidx.annotation.StringRes

data class ProcessingTimelineStep(
    val id: Int,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val status: ProcessingTimelineStepStatus,
    val hasConnector: Boolean,
    @ColorRes val connectorColorRes: Int
)

enum class ProcessingTimelineStepStatus {
    COMPLETED,
    ACTIVE,
    PENDING
}
