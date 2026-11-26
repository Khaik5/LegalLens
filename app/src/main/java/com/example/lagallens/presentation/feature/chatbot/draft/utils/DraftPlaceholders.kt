package com.example.lagallens.presentation.feature.chatbot.draft.utils

import androidx.annotation.StringRes
import com.example.lagallens.R

/** A field the draft still leaves to the user, written in square brackets, for example "[Tên người lao động]". */
private val DRAFT_PLACEHOLDER = Regex("""\[[^\[\]\n]+\]""")

fun String.hasDraftPlaceholders(): Boolean = DRAFT_PLACEHOLDER.containsMatchIn(this)

/** The draft screens warn about [square brackets] only when the draft still has one; otherwise they only ask for a legal review. */
@StringRes
fun draftWarningRes(content: String?): Int =
    if (content == null || content.hasDraftPlaceholders()) R.string.chatbot_draft_missing_warning else R.string.chatbot_draft_review_warning

@StringRes
fun draftPdfWarningRes(content: String?): Int =
    if (content.isNullOrBlank() || content.hasDraftPlaceholders()) R.string.draft_pdf_warning else R.string.draft_pdf_review_warning

@StringRes
fun savedDraftWarningRes(content: String?): Int =
    if (content.isNullOrBlank() || content.hasDraftPlaceholders()) R.string.draft_save_legal_warning else R.string.draft_save_review_warning
