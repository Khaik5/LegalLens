package com.example.lagallens.domain.model

enum class DraftStage(val legacyKey: String) {
    ORIGINAL("D1"),
    REGENERATED("D1-R"),
    MANUAL_EDIT("M1"),
    AI_PROPOSAL("D2"),
    COPY("D3");

    companion object {
        fun fromLegacyKey(value: String): DraftStage? = entries.firstOrNull { it.legacyKey == value }
    }
}

data class Draft(
    val draftId: String,
    val contractId: String?,
    val title: String,
    val content: String,
    val stage: DraftStage,
    val versionNo: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val parentDraftId: String? = null,
    val lineageId: String = draftId
)
