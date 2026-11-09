package com.example.lagallens.presentation.feature.chatbot.draft.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.domain.model.DraftStage
import com.example.lagallens.domain.repository.latestDraft
import com.example.lagallens.presentation.feature.chatbot.draft.contract.CompareDraftVersionsUiEvent
import com.example.lagallens.presentation.feature.chatbot.draft.contract.CompareDraftVersionsUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CompareDraftVersionsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LegalLensApplication
    private val state = MutableStateFlow(CompareDraftVersionsUiState())
    val uiState = state.asStateFlow()
    private var isStarted = false

    fun onEvent(event: CompareDraftVersionsUiEvent) {
        when (event) {
            is CompareDraftVersionsUiEvent.Opened -> if (!isStarted) {
                isStarted = true
                load(event.contractId)
            }
        }
    }

    private fun load(contractId: String?) {
        viewModelScope.launch {
            state.value = try {
                val repository = app.draftRepository.forSession(app.currentSessionKey())
                val first = repository.latestDraft(DraftStage.ORIGINAL, contractId)
                val second = repository.latestDraft(DraftStage.AI_PROPOSAL, contractId)
                if (first == null || second == null) CompareDraftVersionsUiState(isUnavailable = true)
                else firstDifference(first.content, second.content)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                CompareDraftVersionsUiState(isUnavailable = true)
            }
        }
    }

    /** Sections are blank-line separated; the first line of the changed section names the clause. */
    private fun firstDifference(before: String, after: String): CompareDraftVersionsUiState {
        val beforeSections = before.split("\n\n")
        val afterSections = after.split("\n\n")
        val index = beforeSections.indices.firstOrNull { beforeSections[it] != afterSections.getOrNull(it) }
            ?: return CompareDraftVersionsUiState(before = before, after = after)
        val changedBefore = beforeSections[index].lines()
        val changedAfter = afterSections.getOrNull(index).orEmpty().lines()
        return CompareDraftVersionsUiState(
            clause = changedBefore.first(),
            before = changedBefore.drop(1).joinToString("\n"),
            after = changedAfter.drop(1).joinToString("\n")
        )
    }
}
