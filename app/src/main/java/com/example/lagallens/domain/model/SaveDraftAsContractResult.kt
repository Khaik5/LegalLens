package com.example.lagallens.domain.model

/** What happened when a draft version was saved as a contract. A transient failure is thrown, so it can be retried. */
sealed interface SaveDraftAsContractResult {
    data class Saved(val contractId: String) : SaveDraftAsContractResult

    /** This draft can never be saved as a contract: an unknown draft id, or a data source without contracts. */
    data object Unsupported : SaveDraftAsContractResult

    /** The backend refused this version (for example it is empty); repeating the same request cannot succeed. */
    data object Rejected : SaveDraftAsContractResult

    /** This version was already saved once and that contract was deleted; the backend keeps refusing a second save. */
    data object ContractDeleted : SaveDraftAsContractResult

    /** The backend saves a draft's newest version only, and this screen shows an older one. */
    data object NotLatestVersion : SaveDraftAsContractResult
}
