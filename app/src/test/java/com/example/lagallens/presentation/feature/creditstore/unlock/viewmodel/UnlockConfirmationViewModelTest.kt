package com.example.lagallens.presentation.feature.creditstore.unlock.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.presentation.feature.creditstore.unlock.contract.UnlockConfirmationUiEffect
import com.example.lagallens.presentation.feature.creditstore.unlock.contract.UnlockConfirmationUiEvent
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class UnlockConfirmationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun confirmationRequestKeepsTheCreditCalculationAndEmitsOneEffect() = runTest {
        val viewModel = UnlockConfirmationViewModel()
        val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.uiEffect.first() }

        viewModel.onEvent(
            UnlockConfirmationUiEvent.ScreenOpened(
                analysisResultId = "KQ-1042",
                availableCredits = 5,
                unlockCost = 2
            )
        )
        viewModel.onEvent(UnlockConfirmationUiEvent.ConfirmClicked)
        viewModel.onEvent(UnlockConfirmationUiEvent.ConfirmClicked)

        val state = viewModel.uiState.value
        assertTrue(state.isConfirmationRequested)
        assertEquals(5, state.availableCredits)
        assertEquals(2, state.unlockCost)
        assertEquals(3, state.remainingCredits)
        assertEquals(
            UnlockConfirmationUiEffect.UnlockRequested(
                analysisResultId = state.analysisResultId,
                creditCost = state.unlockCost
            ),
            effect.await()
        )
    }

    @Test
    fun confirmationRequestWithInsufficientCreditsEmitsOneRedirectEffect() = runTest {
        val viewModel = UnlockConfirmationViewModel()
        val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.uiEffect.first() }

        viewModel.onEvent(
            UnlockConfirmationUiEvent.ScreenOpened(
                analysisResultId = "KQ-1042",
                availableCredits = 1,
                unlockCost = 2
            )
        )
        viewModel.onEvent(UnlockConfirmationUiEvent.ConfirmClicked)
        viewModel.onEvent(UnlockConfirmationUiEvent.ConfirmClicked)

        assertTrue(viewModel.uiState.value.isInsufficientCreditsRequested)
        assertEquals(UnlockConfirmationUiEffect.NavigateToInsufficientCredits, effect.await())
    }
}
