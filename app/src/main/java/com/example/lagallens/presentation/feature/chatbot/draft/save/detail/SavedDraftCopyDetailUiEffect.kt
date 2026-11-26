package com.example.lagallens.presentation.feature.chatbot.draft.save.detail

import androidx.annotation.StringRes

sealed interface SavedDraftCopyDetailUiEffect {
    data class ShowMessage(@StringRes val messageRes: Int) : SavedDraftCopyDetailUiEffect
    /** The draft is now a contract, so leaving goes to the contract list where it appears. */
    data object NavigateToContracts : SavedDraftCopyDetailUiEffect
    data object Close : SavedDraftCopyDetailUiEffect
}
