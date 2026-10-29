package com.example.lagallens.presentation.feature.chatbot.draft.contract

enum class CopyDraftVersionStatus { LOADING, ERROR, SUCCESS }

data class CopyDraftVersionUiState(val status: CopyDraftVersionStatus = CopyDraftVersionStatus.LOADING, val draftId: String? = null)
