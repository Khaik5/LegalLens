package com.example.lagallens.presentation.feature.upload

object UploadFileValidator {
    const val MAX_BYTES = 20L * 1024L * 1024L

    /** Stops once the file is known to be too large. */
    fun countBytes(stream: java.io.InputStream, limit: Long = MAX_BYTES): Long {
        val buffer = ByteArray(8 * 1024)
        var total = 0L
        while (total <= limit) {
            val read = stream.read(buffer)
            if (read < 0) break
            total += read
        }
        return total
    }

    fun isSupported(mimeType: String): Boolean = mimeType == PDF_MIME ||
        mimeType == DOCX_MIME || mimeType.startsWith("image/", ignoreCase = true)

    const val PDF_MIME = "application/pdf"
    const val DOCX_MIME = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
}
