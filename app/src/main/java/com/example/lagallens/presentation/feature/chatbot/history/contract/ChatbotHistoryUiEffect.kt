package com.example.lagallens.presentation.feature.chatbot.history.contract

import androidx.annotation.StringRes

sealed interface ChatbotHistoryUiEffect {
    data object NavigateBack : ChatbotHistoryUiEffect
    data class ShowMessage(@StringRes val messageRes: Int) : ChatbotHistoryUiEffect
}
