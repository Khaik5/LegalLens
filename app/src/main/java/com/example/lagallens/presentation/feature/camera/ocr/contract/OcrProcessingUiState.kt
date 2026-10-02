package com.example.lagallens.presentation.feature.camera.ocr.contract

import com.example.lagallens.R
import com.example.lagallens.presentation.common.model.ProcessingTimelineStep
import com.example.lagallens.presentation.common.model.ProcessingTimelineStepStatus

data class OcrProcessingUiState(
    val documentName: String = "",
    val documentDetails: String = "",
    val activeStepId: Int = 2,
    val steps: List<ProcessingTimelineStep> = processingSteps(activeStepId)
) {
    fun withActiveStep(activeStepId: Int): OcrProcessingUiState = copy(
        activeStepId = activeStepId,
        steps = processingSteps(activeStepId)
    )
}

private fun processingSteps(activeStepId: Int): List<ProcessingTimelineStep> = listOf(
    ProcessingTimelineStep(
        id = 1,
        titleRes = R.string.ocr_processing_upload_title,
        descriptionRes = R.string.ocr_processing_upload_body,
        status = stepStatus(id = 1, activeStepId = activeStepId),
        hasConnector = true,
        connectorColorRes = connectorColor(id = 1, activeStepId = activeStepId)
    ),
    ProcessingTimelineStep(
        id = 2,
        titleRes = R.string.ocr_processing_analyzing_title,
        descriptionRes = R.string.ocr_processing_analyzing_body,
        status = stepStatus(id = 2, activeStepId = activeStepId),
        hasConnector = true,
        connectorColorRes = connectorColor(id = 2, activeStepId = activeStepId)
    ),
    ProcessingTimelineStep(
        id = 3,
        titleRes = R.string.ocr_processing_pending_title,
        descriptionRes = R.string.ocr_processing_text_description,
        status = stepStatus(id = 3, activeStepId = activeStepId),
        hasConnector = true,
        connectorColorRes = connectorColor(id = 3, activeStepId = activeStepId)
    ),
    ProcessingTimelineStep(
        id = 4,
        titleRes = R.string.ocr_processing_review_title,
        descriptionRes = R.string.ocr_processing_review_description,
        status = stepStatus(id = 4, activeStepId = activeStepId),
        hasConnector = false,
        connectorColorRes = R.color.ocr_saved_border
    )
)

private fun stepStatus(id: Int, activeStepId: Int): ProcessingTimelineStepStatus = when {
    id < activeStepId -> ProcessingTimelineStepStatus.COMPLETED
    id == activeStepId -> ProcessingTimelineStepStatus.ACTIVE
    else -> ProcessingTimelineStepStatus.PENDING
}

private fun connectorColor(id: Int, activeStepId: Int): Int = if (id < activeStepId) {
    R.color.ocr_saved_teal
} else {
    R.color.ocr_saved_border
}
