package com.example.lagallens.domain.model

data class EmploymentDocument(
    val id: String,
    val contractId: String?,
    val displayName: String,
    val uri: String?,
    val mimeType: String,
    val content: String,
    val updatedAtMillis: Long
)
