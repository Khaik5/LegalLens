package com.example.lagallens.presentation.common.utils

import androidx.annotation.StringRes
import com.example.lagallens.R
import com.example.lagallens.domain.model.DraftAiFailedException
import com.example.lagallens.domain.model.DraftAiFailureReason

/** The explanation shown for a failed draft AI request; null means the screen keeps its own generic text. */
@StringRes
internal fun Throwable?.toDraftAiFailureMessage(): Int? =
    when ((this as? DraftAiFailedException)?.reason) {
        DraftAiFailureReason.TIMEOUT -> R.string.draft_ai_failure_timeout
        DraftAiFailureReason.OVERLOADED -> R.string.draft_ai_failure_overloaded
        DraftAiFailureReason.PRIVACY_PROTECTION -> R.string.draft_ai_failure_privacy
        DraftAiFailureReason.SOURCE_UNAVAILABLE -> R.string.draft_ai_failure_source_unavailable
        DraftAiFailureReason.OUT_OF_DATE -> R.string.draft_ai_failure_out_of_date
        DraftAiFailureReason.OTHER, null -> null
    }
