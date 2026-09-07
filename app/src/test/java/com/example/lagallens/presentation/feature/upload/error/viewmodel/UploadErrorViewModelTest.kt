package com.example.lagallens.presentation.feature.upload.error.viewmodel

import com.example.lagallens.presentation.feature.upload.error.contract.UploadErrorEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class UploadErrorViewModelTest {
    @Test
    fun screenOpenedRendersTheProvidedErrorDetails() {
        val viewModel = UploadErrorViewModel()

        viewModel.onEvent(
            UploadErrorEvent.ScreenOpened(
                errorCode = "FILE_TOO_LARGE",
                errorMessage = "Tệp vượt quá kích thước cho phép."
            )
        )

        assertEquals("FILE_TOO_LARGE", viewModel.uiState.value.errorCode)
        assertEquals("Tệp vượt quá kích thước cho phép.", viewModel.uiState.value.errorMessage)
    }
}
