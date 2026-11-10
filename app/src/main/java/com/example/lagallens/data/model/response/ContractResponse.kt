package com.example.lagallens.data.model.response

data class ContractResponse(
    val id: Long,
    val title: String,
    val status: String,
    val description: String?,
    val createdAt: String,
    val updatedAt: String,
    val latestRiskLevel: String? = null,
    val latestAnalysisStatus: String? = null
)

data class ContractPageResponse(
    val items: List<ContractResponse>,
    val total: Long,
    val limit: Int
)
