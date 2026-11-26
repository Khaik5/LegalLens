package com.example.lagallens.presentation.feature.chatbot.draft.utils

import com.example.lagallens.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftPlaceholdersTest {
    @Test fun aBracketedFieldIsAPlaceholder() {
        assertTrue("Bên B: [Tên người lao động]".hasDraftPlaceholders())
        assertEquals(R.string.chatbot_draft_missing_warning, draftWarningRes("Lương: [chưa cung cấp]"))
        assertEquals(R.string.draft_save_legal_warning, savedDraftWarningRes("Lương: [chưa cung cấp]"))
    }

    @Test fun aCompleteDraftOnlyAsksForAReview() {
        val complete = "Điều 4. Thời gian thử việc 60 ngày, lương thử việc bằng 85% lương chính thức."
        assertFalse(complete.hasDraftPlaceholders())
        assertFalse("Hai dòng [mở\nngoặc] không phải chỗ trống".hasDraftPlaceholders())
        assertEquals(R.string.chatbot_draft_review_warning, draftWarningRes(complete))
        assertEquals(R.string.draft_save_review_warning, savedDraftWarningRes(complete))
    }

    @Test fun thePdfWarningFollowsTheDraftText() {
        assertEquals(R.string.draft_pdf_warning, draftPdfWarningRes("Bên B: [Tên]"))
        assertEquals(R.string.draft_pdf_review_warning, draftPdfWarningRes("Điều 1. Thời hạn 12 tháng."))
        assertEquals(R.string.draft_pdf_warning, draftPdfWarningRes(null))
    }

    @Test fun aDraftNotLoadedYetKeepsTheFullWarning() {
        assertEquals(R.string.chatbot_draft_missing_warning, draftWarningRes(null))
        assertEquals(R.string.draft_save_legal_warning, savedDraftWarningRes(""))
    }
}
