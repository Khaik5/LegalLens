package com.example.lagallens.presentation.feature.ocr.contract

import androidx.annotation.StringRes
import com.example.lagallens.R

data class OcrReviewUiState(
    val warnings: List<OcrReviewWarning> = OcrReviewSample.warnings
)

data class OcrReviewWarning(
    @StringRes val sectionRes: Int,
    @StringRes val confidenceRes: Int,
    @StringRes val recognizedValueRes: Int,
    @StringRes val sourceRes: Int
)

object OcrReviewSample {
    val warnings = listOf(
        OcrReviewWarning(
            sectionRes = R.string.ocr_review_warning_one_section,
            confidenceRes = R.string.ocr_review_warning_one_confidence,
            recognizedValueRes = R.string.ocr_review_warning_one_recognized_value,
            sourceRes = R.string.ocr_review_warning_one_source
        ),
        OcrReviewWarning(
            sectionRes = R.string.ocr_review_warning_two_section,
            confidenceRes = R.string.ocr_review_warning_two_confidence,
            recognizedValueRes = R.string.ocr_review_warning_two_recognized_value,
            sourceRes = R.string.ocr_review_warning_two_source
        )
    )
}
