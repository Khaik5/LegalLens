package com.example.lagallens.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnsavedUploadContractsTest {
    @Test fun aMarkedContractCanBeTakenOnlyOnce() {
        val unsaved = UnsavedUploadContracts().apply { markUnsaved("7") }

        assertTrue(unsaved.takeIfUnsaved("7"))
        assertFalse(unsaved.takeIfUnsaved("7"))
    }

    @Test fun aContractThatWasNeverMarkedIsNeverTaken() {
        assertFalse(UnsavedUploadContracts().takeIfUnsaved("7"))
    }

    @Test fun aSavedContractIsNoLongerTemporary() {
        val unsaved = UnsavedUploadContracts().apply { markUnsaved("7") }

        unsaved.markSaved("7")

        assertFalse(unsaved.takeIfUnsaved("7"))
    }
}
