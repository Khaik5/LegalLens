package com.example.lagallens.presentation.feature.ocr.failure.viewmodel

import com.example.lagallens.presentation.feature.ocr.failure.contract.OcrFailureUiEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OcrFailureViewModelTest {
    @Before fun useTestMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun restoreMain() = Dispatchers.resetMain()

    @Test
    fun screenOpenedPreservesDocumentMetadataForRetry() {
        val viewModel = OcrFailureViewModel()

        viewModel.onEvent(
            OcrFailureUiEvent.ScreenOpened(
                documentName = "hop-dong-lao-dong.pdf",
                documentDetails = "3 trang"
            )
        )

        assertEquals("hop-dong-lao-dong.pdf", viewModel.uiState.value.documentName)
        assertEquals("3 trang", viewModel.uiState.value.documentDetails)
    }

    @Test fun leavingTheFailureScreenDiscardsTheUploadContractOnce() {
        val discarded = mutableListOf<String?>()
        val viewModel = OcrFailureViewModel { discarded += it }
        viewModel.onEvent(OcrFailureUiEvent.ScreenOpened("a.png", "PNG", contractId = "9"))

        viewModel.onEvent(OcrFailureUiEvent.BackClicked)
        viewModel.onEvent(OcrFailureUiEvent.BackClicked)

        assertEquals(listOf<String?>("9"), discarded)
    }

    @Test fun retryingKeepsTheUploadContract() {
        val discarded = mutableListOf<String?>()
        val viewModel = OcrFailureViewModel { discarded += it }
        viewModel.onEvent(OcrFailureUiEvent.ScreenOpened("a.png", "PNG", contractId = "9"))

        viewModel.onEvent(OcrFailureUiEvent.RetryClicked)

        assertEquals(emptyList<String?>(), discarded)
    }
}
