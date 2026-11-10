package com.example.lagallens.data.mapper

import com.example.lagallens.core.network.DocumentFiles
import com.example.lagallens.data.model.response.DocumentResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DocumentMapperTest {
    private fun response(fileName: String, fileType: String) =
        DocumentResponse(7, 3, fileName, fileType, 1024, "2026-11-10T09:30:00.000Z")

    @Test fun mapsIdentityNameAndUploadTime() {
        val document = response("Hợp đồng.pdf", "PDF").toDocument()

        assertEquals("7", document.id)
        assertEquals("3", document.contractId)
        assertEquals("Hợp đồng.pdf", document.displayName)
        assertNull(document.uri)
        assertEquals(1_794_303_000_000L, document.updatedAtMillis)
    }

    @Test fun mapsBackendFileTypesToMimeTypes() {
        assertEquals(DocumentFiles.PDF, response("a.pdf", "PDF").toDocument().mimeType)
        assertEquals(DocumentFiles.DOC, response("a.DOC", "WORD").toDocument().mimeType)
        assertEquals(DocumentFiles.DOCX, response("a.docx", "WORD").toDocument().mimeType)
        assertEquals(DocumentFiles.PNG, response("a.png", "IMAGE").toDocument().mimeType)
        assertEquals(DocumentFiles.JPEG, response("a.jpg", "IMAGE").toDocument().mimeType)
    }

    @Test fun unknownTypeFallsBackToFileExtension() {
        assertEquals(DocumentFiles.PDF, response("a.pdf", "OTHER").toDocument().mimeType)
        assertEquals("application/octet-stream", response("a.bin", "OTHER").toDocument().mimeType)
    }
}
