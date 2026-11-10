package com.example.lagallens.core.network

object DocumentFiles {
    const val MAX_UPLOAD_BYTES = 10L * 1024 * 1024
    const val PDF = "application/pdf"
    const val DOC = "application/msword"
    const val DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    const val JPEG = "image/jpeg"
    const val PNG = "image/png"
    private val supportedTypes = setOf(PDF, DOC, DOCX, JPEG, PNG)

    fun mimeType(fileName: String): String? = when (fileName.substringAfterLast('.', "").lowercase()) {
        "pdf" -> PDF
        "doc" -> DOC
        "docx" -> DOCX
        "jpg", "jpeg" -> JPEG
        "png" -> PNG
        else -> null
    }

    fun isSupported(mimeType: String): Boolean = mimeType in supportedTypes
}
