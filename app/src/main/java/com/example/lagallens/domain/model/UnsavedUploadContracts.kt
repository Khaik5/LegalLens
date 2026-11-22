package com.example.lagallens.domain.model

import java.util.concurrent.ConcurrentHashMap

/**
 * Contracts the app created for an upload that the user has not saved yet. Only these may be removed when the
 * recognized text is empty and the user leaves: a contract is never judged temporary by its title, status or an
 * empty result. The set lives in memory, so after the process dies nothing is removed.
 */
class UnsavedUploadContracts {
    private val ids: MutableSet<String> = ConcurrentHashMap.newKeySet()

    fun markUnsaved(contractId: String) { ids += contractId }

    /** True when [contractId] was marked and not saved or discarded since; the mark is consumed. */
    fun takeIfUnsaved(contractId: String): Boolean = ids.remove(contractId)

    fun markSaved(contractId: String) { ids -= contractId }
}
