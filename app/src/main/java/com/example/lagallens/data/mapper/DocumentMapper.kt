package com.example.lagallens.data.mapper

import com.example.lagallens.core.network.DocumentFiles
import com.example.lagallens.data.model.response.DocumentResponse
import com.example.lagallens.domain.model.EmploymentDocument
import java.time.Instant

internal fun DocumentResponse.toDocument() = EmploymentDocument(
    id = id.toString(),
    contractId = contractId.toString(),
    displayName = fileName,
    uri = null,
    mimeType = mimeType?.takeIf(DocumentFiles::isSupported) ?: when (fileType) {
        "PDF" -> DocumentFiles.PDF
        "WORD" -> if (fileName.endsWith(".doc", ignoreCase = true)) DocumentFiles.DOC else DocumentFiles.DOCX
        "IMAGE" -> if (fileName.endsWith(".png", ignoreCase = true)) DocumentFiles.PNG else DocumentFiles.JPEG
        else -> DocumentFiles.mimeType(fileName) ?: "application/octet-stream"
    },
    content = "",
    updatedAtMillis = Instant.parse(uploadedAt).toEpochMilli()
)
