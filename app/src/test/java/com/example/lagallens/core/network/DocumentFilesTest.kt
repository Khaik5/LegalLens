package com.example.lagallens.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentFilesTest {
    @Test fun heicAndWebpAreAcceptedAndUploadedAsJpeg() {
        listOf(DocumentFiles.HEIC, DocumentFiles.HEIF, DocumentFiles.WEBP).forEach { type ->
            assertTrue(DocumentFiles.isAccepted(type))
            assertFalse(DocumentFiles.isSupported(type))
            assertEquals(DocumentFiles.JPEG, DocumentFiles.uploadMimeType(type))
            assertTrue(DocumentFiles.supportsOcr(type))
        }
        assertEquals(DocumentFiles.HEIC, DocumentFiles.mimeType("IMG_0001.HEIC"))
    }

    @Test fun pdfImagesAndWordGoThroughOcr() {
        assertTrue(DocumentFiles.supportsOcr(DocumentFiles.PDF))
        assertTrue(DocumentFiles.supportsOcr(DocumentFiles.PNG))
        assertTrue(DocumentFiles.supportsOcr(DocumentFiles.DOCX))
        assertTrue(DocumentFiles.supportsOcr(DocumentFiles.DOC))
        assertFalse(DocumentFiles.supportsOcr("text/plain"))
        assertEquals(DocumentFiles.PDF, DocumentFiles.uploadMimeType(DocumentFiles.PDF))
    }

    @Test fun uploadLimitMatchesTheBackend() {
        assertEquals(20L * 1024 * 1024, DocumentFiles.MAX_UPLOAD_BYTES)
    }
}
