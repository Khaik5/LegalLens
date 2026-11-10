package com.example.lagallens.data.model.response

data class DocumentResponse(
    val id: Long,
    val contractId: Long,
    val fileName: String,
    val fileType: String,
    val fileSize: Long,
    val uploadedAt: String
)
