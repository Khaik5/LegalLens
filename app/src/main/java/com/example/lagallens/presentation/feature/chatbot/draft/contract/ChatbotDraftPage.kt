package com.example.lagallens.presentation.feature.chatbot.draft.contract

import androidx.annotation.StringRes

data class ChatbotDraftSection(
    @param:StringRes val titleRes: Int,
    @param:StringRes val bodyRes: Int,
    val bodyOverride: String? = null
)

data class ChatbotDraftPage(
    @param:StringRes val headingRes: Int? = null,
    @param:StringRes val introductionRes: Int? = null,
    val sections: List<ChatbotDraftSection>
)
