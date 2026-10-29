package com.example.lagallens.presentation.feature.chatbot.draft

import android.content.Context
import com.example.lagallens.R
import com.example.lagallens.domain.model.DraftStage

internal object DraftContentFactory {
    fun create(context: Context, stage: DraftStage, manualContent: String? = null): String = buildString {
        appendLine(context.getString(R.string.chatbot_draft_contract_heading))
        sections(context, stage, manualContent).forEach { (title, content) ->
            appendLine()
            appendLine(context.getString(title))
            appendLine(content)
        }
    }.trim()

    fun applyAiProposal(context: Context, sourceContent: String): String {
        val sectionTitle = context.getString(R.string.chatbot_draft_section_3_title)
        val nextSectionTitle = context.getString(R.string.chatbot_draft_section_4_title)
        val start = sourceContent.indexOf(sectionTitle)
        val next = if (start >= 0) sourceContent.indexOf(nextSectionTitle, start + sectionTitle.length) else -1
        val replacement = "$sectionTitle\n${context.getString(R.string.chatbot_draft_section_3_approved_body)}\n\n"
        if (start < 0 || next < 0) return sourceContent
        return sourceContent.replaceRange(start, next, replacement)
    }

    private fun sections(context: Context, stage: DraftStage, manualContent: String?) = listOf(
        R.string.chatbot_draft_section_1_title to R.string.chatbot_draft_section_1_body,
        R.string.chatbot_draft_section_2_title to R.string.chatbot_draft_section_2_body,
        R.string.chatbot_draft_section_3_title to if (stage == DraftStage.AI_PROPOSAL) R.string.chatbot_draft_section_3_approved_body else R.string.chatbot_draft_section_3_body,
        R.string.chatbot_draft_section_4_title to R.string.chatbot_draft_section_4_body,
        R.string.chatbot_draft_section_5_title to R.string.chatbot_draft_section_5_body,
        R.string.chatbot_draft_section_6_title to R.string.chatbot_draft_section_6_body,
        R.string.chatbot_draft_section_7_title to R.string.chatbot_draft_section_7_body,
        R.string.chatbot_draft_section_8_title to R.string.chatbot_draft_section_8_body,
        R.string.chatbot_draft_section_9_title to R.string.chatbot_draft_section_9_body
    ).map { (title, body) ->
        title to if (title == R.string.chatbot_draft_section_2_title && stage == DraftStage.MANUAL_EDIT && !manualContent.isNullOrBlank()) {
            manualContent
        } else {
            context.getString(body)
        }
    }
}
