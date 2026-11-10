package com.example.lagallens.data.model.response

data class DocumentResponse(
    val id: Long,
    val contractId: Long,
    val fileName: String,
    val fileType: String,
    val fileSize: Long,
    val uploadedAt: String,
    /** Sent by newer backends; older responses leave it null. */
    val mimeType: String? = null,
    /** Pages of a PDF, 1 for an image, null for Word or when the backend could not count them. */
    val pageCount: Int? = null
)
