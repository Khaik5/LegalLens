package com.example.lagallens.presentation.feature.chatbot.contract

import androidx.annotation.StringRes

sealed interface ChatbotUiEffect {
    data class ShowDraftPreview(val draftId: String?) : ChatbotUiEffect
    data object NavigateBack : ChatbotUiEffect
    data object NavigateToHistory : ChatbotUiEffect
    data object NavigateToHelp : ChatbotUiEffect
    data class NavigateToLegalReference(val referenceId: String) : ChatbotUiEffect
    data object OpenAttachmentPicker : ChatbotUiEffect
    data object OpenAttachmentSheet : ChatbotUiEffect
    data object OpenCamera : ChatbotUiEffect
    data class ShowMessage(@StringRes val messageRes: Int) : ChatbotUiEffect
}
