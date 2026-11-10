package com.example.lagallens.domain.model

enum class EmploymentContractStatus { COMPLETE, PROCESSING, DRAFT }

enum class EmploymentContractRisk { LOW, MEDIUM, HIGH, CRITICAL, NOT_ANALYZED }

enum class EmploymentContractType { EMPLOYMENT, SERVICE, COMMERCIAL }

data class EmploymentContract(
    val id: String,
    val documentId: String,
    val title: String,
    val content: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val status: EmploymentContractStatus = EmploymentContractStatus.DRAFT,
    val risk: EmploymentContractRisk = EmploymentContractRisk.NOT_ANALYZED,
    val fileName: String = "",
    val fileUri: String? = null,
    val mimeType: String = "",
    val type: EmploymentContractType = EmploymentContractType.EMPLOYMENT,
    val pageCount: Int? = null,
    val generatedTitleKey: String? = null,
    val description: String? = null
)

data class EmploymentContractComparison(
    val first: EmploymentContract,
    val second: EmploymentContract,
    val differences: List<ContractTextDifference>
)

data class ContractTextDifference(
    val section: String,
    val firstText: String,
    val secondText: String
)
