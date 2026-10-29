package com.example.lagallens.presentation.feature.chatbot.contract

import androidx.annotation.StringRes

sealed interface ChatbotUiEffect {
    data object ShowDraftPreview : ChatbotUiEffect
    data object NavigateBack : ChatbotUiEffect
    data object NavigateToHistory : ChatbotUiEffect
    data object NavigateToHelp : ChatbotUiEffect
    data object NavigateToLegalReference : ChatbotUiEffect
    data object OpenAttachmentPicker : ChatbotUiEffect
    data object OpenAttachmentSheet : ChatbotUiEffect
    data object OpenCamera : ChatbotUiEffect
    data class ShowMessage(@StringRes val messageRes: Int) : ChatbotUiEffect
}
