package com.example.lagallens.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DraftVersionScopeTest {
    private fun draft(
        id: String,
        contract: String? = null,
        conversation: String? = null,
        lineage: String = id.substringBefore(':'),
        parent: String? = null
    ) = Draft(
        draftId = id, contractId = contract, title = "t", content = "", stage = DraftStage.ORIGINAL, versionNo = 1,
        createdAt = 0, updatedAt = 0, parentDraftId = parent, lineageId = lineage, conversationId = conversation
    )

    private fun List<Draft>.ids(contractId: String?, current: String?) = relatedTo(contractId, current).map { it.draftId }

    @Test fun withoutAContractOrADraftNothingNarrowsTheList() {
        val all = listOf(draft("1:1"), draft("2:1"))

        assertEquals(listOf("1:1", "2:1"), all.ids(null, null))
    }

    @Test fun aContractKeepsOnlyItsOwnDraftsWhenNoDraftIsGiven() {
        val all = listOf(draft("1:1", contract = "A"), draft("2:1", contract = "B"))

        assertEquals(listOf("1:1"), all.ids("A", null))
    }

    @Test fun twoDraftsOfOneContractMadeInDifferentConversationsStaySeparate() {
        val all = listOf(
            draft("1:1", contract = "A", conversation = "c1"), draft("1:2", contract = "A", conversation = "c1"),
            draft("2:1", contract = "A", conversation = "c2"), draft("2:2", contract = "A", conversation = "c2")
        )

        assertEquals(listOf("1:1", "1:2"), all.ids("A", "1:2"))
        assertEquals(listOf("2:1", "2:2"), all.ids("A", "2:1"))
    }

    @Test fun unlinkedDraftsOfDifferentConversationsNeverShowEachOthersVersions() {
        val all = listOf(draft("1:1", conversation = "c1"), draft("2:1", conversation = "c2"))

        assertEquals(listOf("1:1"), all.ids(null, "1:1"))
    }

    @Test fun aRegeneratedDraftOfTheSameConversationBelongsToTheFamily() {
        val all = listOf(draft("1:1", conversation = "c1"), draft("3:1", conversation = "c1"), draft("2:1", conversation = "c2"))

        assertEquals(listOf("1:1", "3:1"), all.ids(null, "1:1"))
    }

    @Test fun aCopyBelongsToItsSourceThroughTheParentLinkEvenWithoutAConversation() {
        val all = listOf(
            draft("1:1"), draft("1:2"), draft("2:1", parent = "1:1"), draft("9:1")
        )

        assertEquals(listOf("1:1", "1:2", "2:1"), all.ids(null, "2:1"))
        assertEquals(listOf("1:1", "1:2", "2:1"), all.ids(null, "1:2"))
    }

    @Test fun versionsOfOneLineageBelongTogetherWithoutAConversation() {
        val all = listOf(draft("1:1"), draft("1:2"), draft("2:1"))

        assertEquals(listOf("1:1", "1:2"), all.ids(null, "1:1"))
    }

    @Test fun theFamilyNeverReachesAnotherContract() {
        val all = listOf(draft("1:1", contract = "A", conversation = "c1"), draft("2:1", contract = "B", conversation = "c1"))

        assertEquals(listOf("1:1"), all.ids("A", "1:1"))
    }

    @Test fun aDraftThatIsNotListedHasNoVersions() {
        val all = listOf(draft("1:1"))

        assertEquals(emptyList<String>(), all.ids(null, "7:1"))
    }
}
