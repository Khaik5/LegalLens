package com.example.lagallens.presentation.feature.analysis.parties.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.analysis.parties.contract.AnalysisPartiesUiEffect
import com.example.lagallens.presentation.feature.analysis.parties.contract.AnalysisPartiesUiEvent
import com.example.lagallens.presentation.feature.analysis.parties.contract.AnalysisPartiesUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AnalysisPartiesViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisPartiesUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<AnalysisPartiesUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun clearResult() { _uiState.value = AnalysisPartiesUiState() }

    fun setResult(identity: com.example.lagallens.domain.model.analysis.AnalysisIdentity, detail: com.example.lagallens.domain.model.analysis.AnalysisDetail, isUnlocked: Boolean = false) {
        val entries = detail.parties.map { party -> com.example.lagallens.presentation.feature.analysis.parties.contract.AnalysisParty(party.id, party.title, party.role, party.fields.map { com.example.lagallens.presentation.feature.analysis.parties.contract.AnalysisPartyField(it.id, it.label, it.value) }) }
        _uiState.update { it.copy(resultId = identity.resultId, parties = entries.take(com.example.lagallens.domain.model.analysis.AnalysisPreview.visibleCount(entries.size, isUnlocked)), lockedCount = entries.size - com.example.lagallens.domain.model.analysis.AnalysisPreview.visibleCount(entries.size, isUnlocked),
            status = if (entries.isEmpty()) com.example.lagallens.presentation.feature.analysis.SnapshotContentStatus.EMPTY else com.example.lagallens.presentation.feature.analysis.SnapshotContentStatus.READY) }
    }

    fun onEvent(event: AnalysisPartiesUiEvent) {
        when (event) {
            is AnalysisPartiesUiEvent.ScreenOpened -> _uiState.update {
                it.copy(
                    documentName = event.documentName,
                    documentMetadata = event.documentMetadata
                )
            }

            AnalysisPartiesUiEvent.BackClicked -> emitEffect(AnalysisPartiesUiEffect.NavigateBack)
        }
    }

    private fun emitEffect(effect: AnalysisPartiesUiEffect) {
        viewModelScope.launch { _uiEffect.send(effect) }
    }
}
