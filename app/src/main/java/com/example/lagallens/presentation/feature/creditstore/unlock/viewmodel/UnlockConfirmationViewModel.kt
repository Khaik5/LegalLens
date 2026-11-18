package com.example.lagallens.presentation.feature.creditstore.unlock.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lagallens.domain.repository.CreditRepository
import com.example.lagallens.presentation.feature.creditstore.unlock.contract.UnlockConfirmationUiEffect
import com.example.lagallens.presentation.feature.creditstore.unlock.contract.UnlockConfirmationUiEvent
import com.example.lagallens.presentation.feature.creditstore.unlock.contract.UnlockConfirmationUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class UnlockConfirmationViewModel(private val creditRepository: CreditRepository,
    private val unlockRepository: com.example.lagallens.domain.repository.AnalysisUnlockRepository? = null) : ViewModel() {
    private val ownerSession = creditRepository.activeSessionKey()
    private val _uiState = MutableStateFlow(UnlockConfirmationUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<UnlockConfirmationUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()
    private var refreshJob: Job? = null
    private var hasKnownBalance = creditRepository.hasCachedBalance

    fun onEvent(event: UnlockConfirmationUiEvent) {
        when (event) {
            is UnlockConfirmationUiEvent.ScreenOpened -> {
                // Activity recreation must not reset an in-flight or completed confirmation.
                if (_uiState.value.isConfirmationRequested) return
                if (creditRepository.hasCachedBalance) hasKnownBalance = true
                _uiState.update {
                    it.copy(analysisResultId = event.analysisResultId,
                        availableCredits = when {
                            !creditRepository.isConfigured -> 0
                            creditRepository.hasCachedBalance -> creditRepository.balance.value
                            else -> it.availableCredits
                        },
                        unlockCost = event.unlockCost.coerceAtLeast(0), isConfirmationRequested = false,
                        isInsufficientCreditsRequested = false,
                        isBackendUnavailable = !creditRepository.isConfigured)
                }
                refreshBalance()
            }

            UnlockConfirmationUiEvent.BackClicked,
            UnlockConfirmationUiEvent.CancelClicked -> emitEffect(UnlockConfirmationUiEffect.NavigateBack)

            UnlockConfirmationUiEvent.ConfirmClicked -> requestUnlock()
        }
    }

    /**
     * The cached balance is 0 until the wallet has been loaded once, and this screen can be opened straight from
     * an analysis result. The real balance is read here so "before" and "after" are not shown as 0.
     */
    private fun refreshBalance(completeAfterUnlock: Boolean = false) {
        refreshJob?.cancel()
        val session = ownerSession
        if (!creditRepository.isConfigured || session.isNullOrBlank() || creditRepository.activeSessionKey() != session) return
        // Enter the protected refresh before UnlockRequested can close the Activity.
        refreshJob = viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            val loaded = try {
                if (completeAfterUnlock) {
                    withContext(NonCancellable) {
                        withTimeout(POST_UNLOCK_REFRESH_TIMEOUT_MILLIS) { creditRepository.refresh() }
                    }
                } else creditRepository.refresh()
                true
            }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { false }
            ensureActive()
            if (creditRepository.activeSessionKey() != session) return@launch
            val previouslyKnown = hasKnownBalance
            hasKnownBalance = loaded || hasKnownBalance || creditRepository.hasCachedBalance
            _uiState.update {
                it.copy(
                    availableCredits = if (loaded || (!previouslyKnown && creditRepository.hasCachedBalance)) {
                        creditRepository.balance.value
                    } else it.availableCredits,
                    isBackendUnavailable = !hasKnownBalance
                )
            }
        }
    }

    private fun requestUnlock() {
        val session = ownerSession
        val state = _uiState.value
        val service = unlockRepository
        if (session.isNullOrBlank() || creditRepository.activeSessionKey() != session || state.analysisResultId.isBlank() || state.unlockCost <= 0 || service == null) {
            emitEffect(UnlockConfirmationUiEffect.ShowBackendUnavailable); return
        }
        if (state.isConfirmationRequested || state.isInsufficientCreditsRequested) return
        refreshJob?.cancel()
        _uiState.update { it.copy(isConfirmationRequested = true) }
        viewModelScope.launch {
            val outcome = try { service.unlock(session, state.analysisResultId) }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { com.example.lagallens.domain.repository.AnalysisUnlockOutcome.Unavailable }
            if (creditRepository.activeSessionKey() != session) return@launch
            when (outcome) {
                is com.example.lagallens.domain.repository.AnalysisUnlockOutcome.Unlocked -> {
                    hasKnownBalance = true
                    _uiState.update { it.copy(availableCredits = outcome.balance, isBackendUnavailable = false) }
                    // Refresh the shared wallet without delaying or invalidating the successful unlock.
                    refreshBalance(completeAfterUnlock = true)
                    _uiEffect.send(UnlockConfirmationUiEffect.UnlockRequested(state.analysisResultId, state.unlockCost))
                }
                is com.example.lagallens.domain.repository.AnalysisUnlockOutcome.Insufficient -> {
                    hasKnownBalance = true
                    _uiState.update { it.copy(availableCredits = outcome.balance, isBackendUnavailable = false, isConfirmationRequested = false, isInsufficientCreditsRequested = true) }
                    _uiEffect.send(UnlockConfirmationUiEffect.NavigateToInsufficientCredits)
                }
                com.example.lagallens.domain.repository.AnalysisUnlockOutcome.Unavailable -> {
                    _uiState.update { it.copy(isConfirmationRequested = false) }
                    _uiEffect.send(UnlockConfirmationUiEffect.ShowBackendUnavailable)
                }
            }
        }
    }

    private fun emitEffect(effect: UnlockConfirmationUiEffect) {
        viewModelScope.launch {
            _uiEffect.send(effect)
        }
    }

    private companion object {
        const val POST_UNLOCK_REFRESH_TIMEOUT_MILLIS = 10_000L
    }

    class Factory(private val repository: CreditRepository, private val unlock: com.example.lagallens.domain.repository.AnalysisUnlockRepository? = null) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = UnlockConfirmationViewModel(repository, unlock) as T
    }
}
