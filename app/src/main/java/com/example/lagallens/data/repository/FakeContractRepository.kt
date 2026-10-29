package com.example.lagallens.data.repository

import com.example.lagallens.data.datasource.local.datastore.EmploymentDemoDataSource
import com.example.lagallens.domain.model.ContractTextDifference
import com.example.lagallens.domain.model.EmploymentContract
import com.example.lagallens.domain.model.EmploymentContractComparison
import com.example.lagallens.domain.repository.ContractRepository
import kotlinx.coroutines.flow.Flow

class FakeContractRepository(private val dataSource: EmploymentDemoDataSource) : ContractRepository {
    override fun observeContracts(): Flow<List<EmploymentContract>> = dataSource.contracts
    override suspend fun getContract(contractId: String): EmploymentContract? = dataSource.getContract(contractId)
    override suspend fun createContract(contract: EmploymentContract): EmploymentContract = dataSource.createContract(contract)
    override suspend fun updateContract(contract: EmploymentContract): Boolean = dataSource.updateContract(contract)
    override suspend fun renameContract(contractId: String, title: String): Boolean = dataSource.renameContract(contractId, title)
    override suspend fun deleteContract(contractId: String): Boolean = dataSource.deleteContract(contractId)

    override suspend fun compareContracts(firstId: String, secondId: String): EmploymentContractComparison? {
        if (firstId == secondId) return null
        val first = getContract(firstId) ?: return null
        val second = getContract(secondId) ?: return null
        val firstSections = first.content.toSections()
        val secondSections = second.content.toSections()
        val sectionOrder = (firstSections.keys + secondSections.keys).distinct()
        val differences = sectionOrder.mapNotNull { section ->
            val left = firstSections[section].orEmpty()
            val right = secondSections[section].orEmpty()
            if (left == right) null else ContractTextDifference(section, left, right)
        }
        return EmploymentContractComparison(first, second, differences)
    }

    private fun String.toSections(): LinkedHashMap<String, String> {
        val sections = linkedMapOf<String, String>()
        var current = "Nội dung"
        val body = StringBuilder()
        lines().forEach { line ->
            val heading = line.trim()
            val isHeading = heading.startsWith("ĐIỀU ", true) || heading.startsWith("ARTICLE ", true)
            if (isHeading) {
                if (body.isNotEmpty()) sections[current] = body.toString().trim()
                current = heading
                body.clear()
            } else if (heading.isNotEmpty()) {
                if (body.isNotEmpty()) body.append('\n')
                body.append(heading)
            }
        }
        if (body.isNotEmpty()) sections[current] = body.toString().trim()
        return sections
    }
}
