package com.example.lagallens.presentation.feature.upload

object UploadFileValidator {
    const val MAX_BYTES = 20L * 1024L * 1024L

    fun isSupported(mimeType: String): Boolean = mimeType == PDF_MIME ||
        mimeType == DOCX_MIME || mimeType.startsWith("image/", ignoreCase = true)

    const val PDF_MIME = "application/pdf"
    const val DOCX_MIME = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
}
