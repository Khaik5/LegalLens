package com.example.lagallens.presentation.feature.ocr.saved.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.presentation.feature.ocr.saved.contract.OcrSavedUiEffect
import com.example.lagallens.presentation.feature.ocr.saved.contract.OcrSavedUiEvent
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OcrSavedIdentityTest {
    @get:Rule val dispatcher = MainDispatcherRule()

    @Test fun analyzeCarriesDocumentAndContractIdentity() = runTest {
        val vm = OcrSavedViewModel()
        vm.onEvent(OcrSavedUiEvent.ScreenOpened("same name", "v1", "saved", "doc-1", "contract-1"))
        val effect = async(start = CoroutineStart.UNDISPATCHED) { vm.uiEffect.first() }
        vm.onEvent(OcrSavedUiEvent.AnalyzeClicked)
        assertEquals(OcrSavedUiEffect.NavigateToAnalysis("same name", "doc-1", "contract-1"), effect.await())
    }
}
