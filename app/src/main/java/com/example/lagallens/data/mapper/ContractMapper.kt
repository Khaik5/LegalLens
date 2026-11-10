package com.example.lagallens.data.mapper

import com.example.lagallens.domain.model.EmploymentContract
import com.example.lagallens.domain.model.EmploymentContractRisk
import com.example.lagallens.domain.model.EmploymentContractStatus
import com.example.lagallens.data.model.response.ContractResponse
import java.time.Instant

internal fun ContractResponse.toContract() = EmploymentContract(
    id = id.toString(),
    documentId = "",
    title = title,
    content = "",
    createdAtMillis = Instant.parse(createdAt).toEpochMilli(),
    updatedAtMillis = Instant.parse(updatedAt).toEpochMilli(),
    status = when (latestAnalysisStatus) {
        "PENDING", "PROCESSING" -> EmploymentContractStatus.PROCESSING
        "COMPLETED" -> EmploymentContractStatus.COMPLETE
        "FAILED" -> EmploymentContractStatus.DRAFT
        else -> when (status) {
            "PENDING_REVIEW" -> EmploymentContractStatus.PROCESSING
            "ACTIVE", "EXPIRED", "TERMINATED" -> EmploymentContractStatus.COMPLETE
            else -> EmploymentContractStatus.DRAFT
        }
    },
    risk = when (latestRiskLevel) {
        "VERY_LOW", "LOW" -> EmploymentContractRisk.LOW
        "MEDIUM" -> EmploymentContractRisk.MEDIUM
        "HIGH" -> EmploymentContractRisk.HIGH
        "CRITICAL" -> EmploymentContractRisk.CRITICAL
        else -> EmploymentContractRisk.NOT_ANALYZED
    },
    description = description
)
