package com.example.lagallens.presentation.feature.creditstore.locked.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.domain.usecase.GetLockedAnalysisResultsUseCase
import com.example.lagallens.presentation.feature.creditstore.locked.contract.LockedResultsUiEffect
import com.example.lagallens.presentation.feature.creditstore.locked.contract.LockedResultsUiEvent
import com.example.lagallens.presentation.feature.creditstore.locked.contract.LockedResultsUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LockedResultsViewModel(private val getLockedResults: GetLockedAnalysisResultsUseCase) : ViewModel() {
    private val ownerSession = getLockedResults.sessionKey()
    private val _uiState = MutableStateFlow(LockedResultsUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<LockedResultsUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    private var loadJob: Job? = null

    fun isOwnerSession() = ownerSession != null && ownerSession == getLockedResults.sessionKey()

    fun onEvent(event: LockedResultsUiEvent) {
        when (event) {
            // Reloaded on every return, so a result unlocked on the result screen leaves the list.
            LockedResultsUiEvent.ScreenResumed, LockedResultsUiEvent.RetryClicked -> load()
            LockedResultsUiEvent.BackClicked -> _uiEffect.trySend(LockedResultsUiEffect.NavigateBack)
            is LockedResultsUiEvent.ResultClicked -> ownerSession?.takeIf { isOwnerSession() }?.let {
                _uiEffect.trySend(LockedResultsUiEffect.OpenResult(event.result, it))
            }
        }
    }

    private fun load() {
        val session = ownerSession?.takeIf { isOwnerSession() } ?: return
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, errorMessageRes = null) }
        loadJob = viewModelScope.launch {
            try {
                val results = getLockedResults(session)
                if (isOwnerSession()) _uiState.update { it.copy(isLoading = false, results = results) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                if (isOwnerSession()) {
                    _uiState.update { it.copy(isLoading = false, results = emptyList(), errorMessageRes = R.string.locked_results_error) }
                }
            }
        }
    }

    class Factory(private val getLockedResults: GetLockedAnalysisResultsUseCase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = LockedResultsViewModel(getLockedResults) as T
    }
}
