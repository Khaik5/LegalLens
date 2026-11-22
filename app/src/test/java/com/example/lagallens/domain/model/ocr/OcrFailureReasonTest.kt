package com.example.lagallens.domain.model.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrFailureReasonTest {
    @Test fun serviceProblemsAreNotBlamedOnTheDocument() {
        listOf(
            "OCR_ENGINE_NOT_INSTALLED", "OCR_PROVIDER_ERROR", "OCR_PROVIDER_UNAVAILABLE", "OCR_PROVIDER_NOT_CONFIGURED",
            "OCR_HTTP_ERROR", "OCR_INVALID_RESULT", "OCR_LEASE_EXPIRED"
        ).forEach { assertEquals(it, OcrFailureReason.SERVICE_UNAVAILABLE, OcrFailureReason.fromCode(it)) }
    }

    @Test fun aTimeoutIsItsOwnReason() {
        assertEquals(OcrFailureReason.TIMEOUT, OcrFailureReason.fromCode("OCR_PROCESSING_TIMEOUT"))
    }

    @Test fun fileProblemsCannotBeFixedByRetrying() {
        listOf("OCR_UNSUPPORTED_FILE", "OCR_SOURCE_MISSING", "OCR_SOURCE_CHANGED")
            .forEach { assertEquals(it, OcrFailureReason.FILE_PROBLEM, OcrFailureReason.fromCode(it)) }
    }

    @Test fun anUnreadableDocumentAndUnknownCodesKeepTheDocumentWording() {
        assertEquals(OcrFailureReason.UNREADABLE, OcrFailureReason.fromCode("OCR_PROCESSING_FAILED"))
        assertEquals(OcrFailureReason.UNREADABLE, OcrFailureReason.fromCode("SOMETHING_NEW"))
        assertEquals(OcrFailureReason.UNREADABLE, OcrFailureReason.fromCode(null))
    }
}
