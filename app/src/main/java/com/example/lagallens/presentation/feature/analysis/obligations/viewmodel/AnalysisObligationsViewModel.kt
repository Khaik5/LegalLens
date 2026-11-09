package com.example.lagallens.presentation.feature.analysis.obligations.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.analysis.obligations.contract.AnalysisObligationsUiEffect
import com.example.lagallens.presentation.feature.analysis.obligations.contract.AnalysisObligationsUiEvent
import com.example.lagallens.presentation.feature.analysis.obligations.contract.AnalysisObligationsUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AnalysisObligationsViewModel : ViewModel() {
    private var entries: List<com.example.lagallens.domain.model.analysis.AnalysisObligationEntry> = emptyList()
    private var unlocked = false
    private val _uiState = MutableStateFlow(AnalysisObligationsUiState(items = emptyList()))
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<AnalysisObligationsUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun clearResult() { entries = emptyList(); _uiState.value = AnalysisObligationsUiState() }

    fun setResult(identity: com.example.lagallens.domain.model.analysis.AnalysisIdentity, detail: com.example.lagallens.domain.model.analysis.AnalysisDetail, isUnlocked: Boolean = false) {
        if (_uiState.value.resultId != identity.resultId) _uiState.update { it.copy(selectedTab = com.example.lagallens.domain.model.analysis.AnalysisObligationTab.RIGHTS) }
        entries = detail.obligations
        unlocked = isUnlocked
        _uiState.update { it.copy(resultId = identity.resultId) }
        renderTab(_uiState.value.selectedTab)
    }

    private fun renderTab(tab: com.example.lagallens.domain.model.analysis.AnalysisObligationTab) {
        var currentTab = com.example.lagallens.domain.model.analysis.AnalysisObligationTab.RIGHTS
        val inTab = entries.filter { entry ->
            when (entry) {
                is com.example.lagallens.domain.model.analysis.AnalysisObligationEntry.Section -> { currentTab = entry.tab; currentTab == tab }
                is com.example.lagallens.domain.model.analysis.AnalysisObligationEntry.Item -> currentTab == tab
            }
        }
        val total = inTab.count { it is com.example.lagallens.domain.model.analysis.AnalysisObligationEntry.Item }
        val visible = com.example.lagallens.domain.model.analysis.AnalysisPreview.visibleCount(total, unlocked)
        var used = 0
        val items = inTab.mapNotNull { entry ->
            when (entry) {
                is com.example.lagallens.domain.model.analysis.AnalysisObligationEntry.Section -> com.example.lagallens.presentation.feature.analysis.obligations.contract.AnalysisObligationsListItem.Section(entry.id, entry.title)
                is com.example.lagallens.domain.model.analysis.AnalysisObligationEntry.Item -> if (used++ < visible) {
                    val source = entry.source?.let { com.example.lagallens.presentation.feature.analysis.keyfacts.contract.AnalysisClauseSource(it.reference, it.title, it.excerpt) }
                    com.example.lagallens.presentation.feature.analysis.obligations.contract.AnalysisObligationsListItem.Obligation(com.example.lagallens.presentation.feature.analysis.obligations.contract.AnalysisObligation(entry.id, entry.title, entry.clause, entry.description, source))
                } else null
            }
        }
        _uiState.update { it.copy(selectedTab = tab, items = items, lockedCount = total - visible,
            status = if (total == 0) com.example.lagallens.presentation.feature.analysis.SnapshotContentStatus.EMPTY else com.example.lagallens.presentation.feature.analysis.SnapshotContentStatus.READY) }
    }

    fun onEvent(event: AnalysisObligationsUiEvent) {
        when (event) {
            is AnalysisObligationsUiEvent.ScreenOpened -> _uiState.update {
                it.copy(
                    documentName = event.documentName,
                    documentMetadata = event.documentMetadata
                )
            }

            AnalysisObligationsUiEvent.BackClicked -> emitEffect(AnalysisObligationsUiEffect.NavigateBack)
            is AnalysisObligationsUiEvent.TabSelected -> renderTab(event.tab)
            is AnalysisObligationsUiEvent.SourceClicked -> {
                val obligation = _uiState.value.items.filterIsInstance<com.example.lagallens.presentation.feature.analysis.obligations.contract.AnalysisObligationsListItem.Obligation>().firstOrNull { it.obligation.id == event.id }?.obligation
                if (obligation != null) emitEffect(obligation.source?.let { AnalysisObligationsUiEffect.ShowSource(it) } ?: AnalysisObligationsUiEffect.SourceUnavailable)
            }
        }
    }

    private fun emitEffect(effect: AnalysisObligationsUiEffect) {
        viewModelScope.launch { _uiEffect.send(effect) }
    }
}
