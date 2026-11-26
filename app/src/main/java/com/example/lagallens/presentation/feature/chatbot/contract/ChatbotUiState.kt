package com.example.lagallens.presentation.feature.chatbot.contract

enum class DraftGenerationStatus { NONE, LOADING, ERROR, SUCCESS }

data class ChatbotUiState(
    val contractName: String = "",
    val contractType: String = "",
    /** The contract the chat is about; null for a chat without a contract. */
    val sourceContractId: String? = null,
    val messageDraft: String = "",
    val messages: List<ChatbotMessage> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val isConversationReady: Boolean = false,
    val isReplyPending: Boolean = false,
    val isNewConversation: Boolean = false,
    val isDraftConversation: Boolean = false,
    val isDraftPreparing: Boolean = false,
    val isDraftReviewReady: Boolean = false,
    val isDraftReviewVisible: Boolean = false,
    /** The message that started drafting; the review lists only what the user asked after it. */
    val draftRequestMessageId: Long? = null,
    /** The text of the draft just made; it decides whether the result card warns about missing information. */
    val generatedDraftContent: String? = null,
    val draftGenerationStatus: DraftGenerationStatus = DraftGenerationStatus.NONE,
    /** What went wrong when the draft could not be made, if the backend said; null keeps the generic text. */
    val draftGenerationErrorRes: Int? = null,
    val isDrawerVisible: Boolean = false,
    val attachmentUri: String? = null,
    val attachmentName: String? = null,
    val isContractDraftingEnabled: Boolean = true,
    /** The legal article opened from an answer's source list; null while none is open. */
    val legalArticle: LegalArticleSheetState? = null
)

enum class LegalArticleStatus { LOADING, READY, UNAVAILABLE, FAILED }

data class LegalArticleSheetState(
    val articleId: Long,
    val status: LegalArticleStatus = LegalArticleStatus.LOADING,
    val title: String = "",
    val content: String = ""
)
