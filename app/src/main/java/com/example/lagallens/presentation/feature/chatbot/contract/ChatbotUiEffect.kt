package com.example.lagallens.presentation.feature.chatbot.contract

import androidx.annotation.StringRes

sealed interface ChatbotUiEffect {
    data object NavigateBack : ChatbotUiEffect
    data class ShowMessage(@StringRes val messageRes: Int) : ChatbotUiEffect
}
