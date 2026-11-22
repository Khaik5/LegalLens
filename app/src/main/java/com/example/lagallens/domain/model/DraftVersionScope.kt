package com.example.lagallens.domain.model

/**
 * The drafts whose versions belong together on one screen: the family of [currentDraftId], limited to [contractId]'s
 * drafts when there is a contract, so one draft never shows another's versions. A family is every draft that
 * shares a conversation or a lineage with a member, or is the source or the copy of one (regeneration and copies
 * are new drafts of the same conversation or point back to their source).
 *
 * Without [currentDraftId] nothing narrows the list further than the contract.
 */
fun List<Draft>.relatedTo(contractId: String?, currentDraftId: String?): List<Draft> {
    val candidates = if (contractId != null) filter { it.contractId == contractId } else this
    if (currentDraftId == null) return candidates
    val family = candidates.filter { it.draftId == currentDraftId }.toMutableList()
    if (family.isEmpty()) return emptyList()
    do {
        val before = family.size
        val conversations = family.mapNotNull { it.conversationId }.toSet()
        val lineages = family.map { it.lineageId }.toSet()
        val ids = family.map { it.draftId }.toSet()
        val parents = family.mapNotNull { it.parentDraftId }.toSet()
        candidates.filter { it !in family }.forEach { draft ->
            val related = draft.conversationId in conversations || draft.lineageId in lineages ||
                draft.parentDraftId in ids || draft.draftId in parents
            if (related) family += draft
        }
    } while (family.size > before)
    return candidates.filter { it in family }
}
