package com.example.lagallens.presentation.feature.camera.pages.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.domain.model.camera.ScanPage
import com.example.lagallens.presentation.feature.camera.pages.contract.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CameraPageDeletionTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun deletingAPageRequestsFileDeletionWithoutLeavingTheScreen() = runTest {
        val model = CameraPageManagerViewModel()
        model.setPages(listOf(ScanPage("first.jpg"), ScanPage("second.jpg")))
        model.onEvent(CameraPageManagerUiEvent.DeletePageClicked("first.jpg"))
        assertEquals(listOf(ScanPage("second.jpg")), model.uiState.value.pages)
        assertEquals(CameraPageManagerUiEffect.DeletePageFile("first.jpg"), withTimeoutOrNull(100) { model.uiEffect.first() })
    }

    @Test fun unknownDeletionDoesNotDeleteAnUnownedFile() = runTest {
        val model = CameraPageManagerViewModel()
        model.setPages(listOf(ScanPage("owned.jpg")))
        model.onEvent(CameraPageManagerUiEvent.DeletePageClicked("elsewhere.jpg"))
        assertNull(withTimeoutOrNull(100) { model.uiEffect.first() })
        assertEquals(listOf(ScanPage("owned.jpg")), model.uiState.value.pages)
    }
}
