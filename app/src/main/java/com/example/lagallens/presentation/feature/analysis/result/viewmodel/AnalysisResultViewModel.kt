package com.example.lagallens.presentation.feature.analysis.result.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.domain.usecase.GetAnalysisOverviewUseCase
import com.example.lagallens.presentation.feature.analysis.result.contract.AnalysisResultDestination
import com.example.lagallens.presentation.feature.analysis.result.contract.AnalysisResultUiEffect
import com.example.lagallens.presentation.feature.analysis.result.contract.AnalysisResultUiEvent
import com.example.lagallens.presentation.feature.analysis.result.contract.AnalysisResultUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

class AnalysisResultViewModel(
    private val getAnalysisOverview: GetAnalysisOverviewUseCase,
    private val identities: com.example.lagallens.domain.repository.AnalysisIdentityRepository,
    private val unlocks: com.example.lagallens.domain.repository.AnalysisUnlockRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisResultUiState(items = emptyList()))
    private var ownerSession: String? = null
    private var loadGeneration = 0
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<AnalysisResultUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun clearResult() {
        loadGeneration++
        ownerSession = null
        _uiState.value = AnalysisResultUiState(items = emptyList())
    }

    fun onEvent(event: AnalysisResultUiEvent) {
        when (event) {
            is AnalysisResultUiEvent.ScreenOpened -> loadOverview(event.documentName, event.documentMetadata, event.resultId, event.sessionKey)
            AnalysisResultUiEvent.BackClicked -> emitEffect(AnalysisResultUiEffect.NavigateBack)
            is AnalysisResultUiEvent.ItemClicked -> navigateToDestination(event.destination)
            AnalysisResultUiEvent.EntitlementChanged -> refreshEntitlement()
        }
    }

    private fun loadOverview(documentName: String, documentMetadata: String, resultId: String, sessionKey: String) {
        ownerSession = sessionKey
        val generation = ++loadGeneration
        _uiState.value = AnalysisResultUiState(items = emptyList())
        viewModelScope.launch {
            try {
                check(sessionKey.isNotBlank() && identities.sessionKey() == sessionKey)
                val identity = checkNotNull(identities.get(resultId, sessionKey)) { "Analysis unavailable" }
                val overview = getAnalysisOverview(identity)
                check(overview.score in 0..100) { "Analysis score unavailable" }
                val entitlement = unlocks.status(sessionKey, resultId)
                if (generation != loadGeneration || identities.sessionKey() != sessionKey) return@launch
                _uiState.update {
                    it.copy(
                        documentName = documentName,
                        documentMetadata = documentMetadata,
                        score = overview.score,
                        severity = overview.severity,
                        keyFactsCount = overview.keyFactsCount,
                        riskCount = identity.findingIds.size,
                        referenceCount = overview.referenceCount,
                        items = AnalysisResultUiState().items,
                        errorMessageRes = null, isLoading = false,
                        resultId = resultId, isUnlocked = entitlement.isUnlocked, unlockCost = entitlement.cost
                    )
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Exception) {
                if (generation != loadGeneration || identities.sessionKey() != sessionKey) return@launch
                _uiState.update { it.copy(documentName = documentName, documentMetadata = documentMetadata, score = 0, keyFactsCount = 0, riskCount = 0, referenceCount = 0, items = emptyList(), errorMessageRes = R.string.analysis_result_unavailable, isLoading = false) }
            }
        }
    }

    private fun refreshEntitlement() {
        val session = ownerSession
        val resultId = _uiState.value.resultId
        if (session.isNullOrBlank() || resultId.isBlank()) return
        val generation = loadGeneration
        viewModelScope.launch {
            val entitlement = try {
                unlocks.status(session, resultId)
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Exception) {
                return@launch
            }
            if (generation != loadGeneration || identities.sessionKey() != session) return@launch
            _uiState.update { it.copy(isUnlocked = entitlement.isUnlocked, unlockCost = entitlement.cost) }
        }
    }

    private fun navigateToDestination(destination: AnalysisResultDestination) {
        if (ownerSession.isNullOrBlank() || identities.sessionKey() != ownerSession || _uiState.value.items.none { it.destination == destination }) return
        when (destination) {
        AnalysisResultDestination.SUMMARY -> emitEffect(
            AnalysisResultUiEffect.NavigateToSummary(
                documentName = _uiState.value.documentName,
                documentMetadata = _uiState.value.documentMetadata
            )
        )

        AnalysisResultDestination.KEY_FACTS -> emitEffect(
            AnalysisResultUiEffect.NavigateToKeyFacts(
                documentName = _uiState.value.documentName,
                documentMetadata = _uiState.value.documentMetadata
            )
        )

        AnalysisResultDestination.PARTIES -> emitEffect(
            AnalysisResultUiEffect.NavigateToParties(
                documentName = _uiState.value.documentName,
                documentMetadata = _uiState.value.documentMetadata
            )
        )

        AnalysisResultDestination.OBLIGATIONS -> emitEffect(
            AnalysisResultUiEffect.NavigateToObligations(
                documentName = _uiState.value.documentName,
                documentMetadata = _uiState.value.documentMetadata
            )
        )

        AnalysisResultDestination.RISK_OVERVIEW -> emitEffect(
            AnalysisResultUiEffect.NavigateToRiskOverview(
                documentName = _uiState.value.documentName,
                documentMetadata = _uiState.value.documentMetadata
            )
        )

        AnalysisResultDestination.LEGAL_REFERENCES -> emitEffect(
            AnalysisResultUiEffect.NavigateToLegalReferences(
                documentName = _uiState.value.documentName,
                documentMetadata = _uiState.value.documentMetadata
            )
        )
    }

    }

    private fun emitEffect(effect: AnalysisResultUiEffect) {
        val session = ownerSession
        val generation = loadGeneration
        viewModelScope.launch {
            if (effect == AnalysisResultUiEffect.NavigateBack ||
                (generation == loadGeneration && !session.isNullOrBlank() && identities.sessionKey() == session)) _uiEffect.send(effect)
        }
    }

    class Factory(
        private val getAnalysisOverview: GetAnalysisOverviewUseCase,
        private val identities: com.example.lagallens.domain.repository.AnalysisIdentityRepository,
        private val unlocks: com.example.lagallens.domain.repository.AnalysisUnlockRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AnalysisResultViewModel::class.java))
            return AnalysisResultViewModel(getAnalysisOverview, identities, unlocks) as T
        }
    }
}
