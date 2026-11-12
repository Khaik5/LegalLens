package com.example.lagallens.core.network

object DocumentFiles {
    /** Same limit as the backend upload (20 MiB). */
    const val MAX_UPLOAD_BYTES = 20L * 1024 * 1024
    const val PDF = "application/pdf"
    const val DOC = "application/msword"
    const val DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    const val JPEG = "image/jpeg"
    const val PNG = "image/png"
    const val HEIC = "image/heic"
    const val HEIF = "image/heif"
    const val WEBP = "image/webp"
    private val supportedTypes = setOf(PDF, DOC, DOCX, JPEG, PNG)
    /** Photo formats the backend rejects; the app converts them to JPEG before uploading. */
    private val convertedImageTypes = setOf(HEIC, HEIF, WEBP)

    fun mimeType(fileName: String): String? = when (fileName.substringAfterLast('.', "").lowercase()) {
        "pdf" -> PDF
        "doc" -> DOC
        "docx" -> DOCX
        "jpg", "jpeg" -> JPEG
        "png" -> PNG
        "heic" -> HEIC
        "heif" -> HEIF
        "webp" -> WEBP
        else -> null
    }

    /** Types the backend stores as they are. */
    fun isSupported(mimeType: String): Boolean = mimeType in supportedTypes

    fun isConvertedToJpeg(mimeType: String): Boolean = mimeType in convertedImageTypes

    /** Types the user can pick: stored as they are or converted to JPEG first. */
    fun isAccepted(mimeType: String): Boolean = isSupported(mimeType) || isConvertedToJpeg(mimeType)

    /** The type the backend receives for a picked file. */
    fun uploadMimeType(mimeType: String): String = if (isConvertedToJpeg(mimeType)) JPEG else mimeType

    /** The backend recognizes text in PDF, image and Word files; it converts Word files to PDF first. */
    fun supportsOcr(mimeType: String): Boolean = uploadMimeType(mimeType) in supportedTypes
}
