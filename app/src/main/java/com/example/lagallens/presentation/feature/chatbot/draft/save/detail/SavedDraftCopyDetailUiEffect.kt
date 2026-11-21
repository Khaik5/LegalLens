package com.example.lagallens.presentation.feature.chatbot.draft.save.detail

import androidx.annotation.StringRes

sealed interface SavedDraftCopyDetailUiEffect {
    data class ShowMessage(@StringRes val messageRes: Int) : SavedDraftCopyDetailUiEffect
}
