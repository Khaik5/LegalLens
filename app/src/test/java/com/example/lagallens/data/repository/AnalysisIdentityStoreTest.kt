package com.example.lagallens.data.repository

import com.example.lagallens.domain.model.analysis.AnalysisIdentity
import org.junit.Assert.*
import org.junit.Test

class AnalysisIdentityStoreTest {
    @Test fun newAndRepeatedOperationsKeepTheirIdentityAfterReload() {
        var persisted = emptyList<AnalysisIdentity>()
        var sequence = 0
        fun store() = AnalysisIdentityStore({ persisted }, { persisted = it }, { c, d -> c == "c" && d == "d" }, { "r${++sequence}" })
        val first = store().create("op1", "c", "d")
        assertEquals(first, store().create("op1", "c", "d"))
        assertEquals(first, store().get(first.resultId))
        assertNotEquals(first.resultId, store().create("op2", "c", "d").resultId)
        assertNull(store().get("missing"))
    }

    @Test fun failedWriteAndDeletedDocumentDoNotReturnSuccess() {
        var persisted = emptyList<AnalysisIdentity>()
        var exists = true
        val store = AnalysisIdentityStore({ persisted }, { throw IllegalStateException("write failed") }, { _, _ -> exists }, { "r" })
        assertTrue(runCatching { store.create("op", "c", "d") }.isFailure)
        assertTrue(persisted.isEmpty())
        persisted = listOf(AnalysisIdentity("r", "op", "c", "d"))
        exists = false
        assertNull(store.get("r"))
    }

    @Test fun operationCannotBeReusedForAnotherContract() {
        val records = listOf(AnalysisIdentity("r", "op", "a", "da"))
        val store = AnalysisIdentityStore({ records }, { fail("unexpected write") }, { _, _ -> true })
        assertTrue(runCatching { store.create("op", "b", "db") }.isFailure)
    }
    @Test fun eachNewIdentityCarriesItsOwnFixtureSnapshot() {
        var records = emptyList<AnalysisIdentity>()
        var next = 0
        val store = AnalysisIdentityStore({ records }, { records = it }, { _, _ -> true }, { "result-${++next}" })
        val first = store.create("op-a", "contract-a", "doc-a")
        val second = store.create("op-b", "contract-b", "doc-b")
        assertNotEquals(first.resultId, second.resultId)
        assertEquals(listOf("penalty-cap", "termination-notice", "dispute-resolution"), store.get(first.resultId)?.findingIds)
        assertEquals(65, store.get(second.resultId)?.overview?.score)
    }
    @Test fun encodedStorageReloadRetainsIndependentSnapshotsAndDoesNotRecreateThem() {
        var disk = "[]"
        var snapshotCalls = 0
        var next = 0
        fun store() = AnalysisIdentityStore(
            read = { AnalysisIdentityCodec.decode(disk) },
            write = { disk = AnalysisIdentityCodec.encode(it) },
            linkedDocument = { _, _ -> true },
            newId = { "id-${++next}" },
            snapshot = { mapOf("vi:content" to "value-${++snapshotCalls}") }
        )
        val a = store().create("op-a", "same-title-id-a", "doc-a")
        val b = store().create("op-b", "same-title-id-b", "doc-b")
        assertNotEquals(a.snapshot, b.snapshot)
        assertEquals(a, store().get(a.resultId))
        assertEquals(a, store().create("op-a", "same-title-id-a", "doc-a"))
        assertEquals(2, snapshotCalls)
        val legacy = AnalysisIdentityCodec.decode("[{\"resultId\":\"r\",\"operationId\":\"o\",\"contractId\":\"c\",\"documentId\":\"d\"}]").single()
        assertNull(legacy.snapshot)
    }
}
