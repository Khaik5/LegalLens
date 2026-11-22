package com.example.lagallens.domain.usecase

import com.example.lagallens.domain.model.UnsavedUploadContracts
import com.example.lagallens.domain.repository.ContractRepository
import kotlinx.coroutines.CancellationException

/**
 * Removes the contract an upload created when the user abandons that upload. Only a contract this app marked as
 * unsaved is removed, and only once; any other contract id is ignored. A failed removal is reported as false and
 * never thrown, because the user is already leaving the screen.
 */
class DiscardUnsavedUploadUseCase(
    private val unsaved: UnsavedUploadContracts,
    private val contracts: () -> ContractRepository
) {
    suspend operator fun invoke(contractId: String?): Boolean {
        val id = contractId?.takeIf(String::isNotBlank) ?: return false
        if (!unsaved.takeIfUnsaved(id)) return false
        return try {
            contracts().deleteContract(id)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
    }
}
