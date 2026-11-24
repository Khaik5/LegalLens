package com.example.lagallens.presentation.feature.dashboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.domain.model.EmploymentContract
import com.example.lagallens.domain.model.EmploymentContractRisk
import com.example.lagallens.domain.repository.ContractRepository
import com.example.lagallens.domain.repository.ProfileRepository
import com.example.lagallens.presentation.feature.contracts.contract.toContractListItem
import com.example.lagallens.presentation.feature.dashboard.contract.DashboardRecentContract
import com.example.lagallens.presentation.feature.dashboard.contract.DashboardUpcomingDate
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import com.example.lagallens.presentation.feature.dashboard.contract.HomeDashboardUiEffect
import com.example.lagallens.presentation.feature.dashboard.contract.HomeDashboardUiEvent
import com.example.lagallens.presentation.feature.dashboard.contract.HomeDashboardUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeDashboardViewModel(
    private val contracts: ContractRepository,
    private val profiles: ProfileRepository,
    private val today: () -> LocalDate = LocalDate::now
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeDashboardUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<HomeDashboardUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()
    private var contractsJob: Job? = null
    private var profileJob: Job? = null
    private var hasResumedOnce = false

    init {
        observeContracts()
    }

    fun onEvent(event: HomeDashboardUiEvent) {
        when (event) {
            HomeDashboardUiEvent.ScreenResumed -> {
                loadProfile()
                // Coming back to the screen picks up changes made elsewhere, such as on the web; the first show just loaded.
                if (hasResumedOnce) contracts.invalidate() else hasResumedOnce = true
            }
            HomeDashboardUiEvent.RetryClicked -> observeContracts()
            HomeDashboardUiEvent.UploadClicked,
            HomeDashboardUiEvent.AnalyzeClicked -> emitEffect(HomeDashboardUiEffect.NavigateToUploadSource)

            HomeDashboardUiEvent.CameraClicked -> emitEffect(HomeDashboardUiEffect.NavigateToCamera)
            HomeDashboardUiEvent.CompareClicked -> emitEffect(HomeDashboardUiEffect.NavigateToCompare)
            HomeDashboardUiEvent.ViewAllContractsClicked -> emitEffect(HomeDashboardUiEffect.NavigateToContracts)
            is HomeDashboardUiEvent.ContractClicked -> emitEffect(
                HomeDashboardUiEffect.NavigateToContractDetail(event.contractId)
            )
        }
    }

    private fun observeContracts() {
        contractsJob?.cancel()
        _uiState.update { it.copy(isLoading = true, errorMessageRes = null) }
        contractsJob = viewModelScope.launch {
            try {
                contracts.observeContracts().collect(::showContracts)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, recentContracts = emptyList(), errorMessageRes = R.string.dashboard_load_failed)
                }
            }
        }
    }

    private fun upcomingDates(items: List<EmploymentContract>): List<DashboardUpcomingDate> {
        val now = today()
        return items.mapNotNull { contract ->
            val end = contract.endDate ?: return@mapNotNull null
            val daysLeft = ChronoUnit.DAYS.between(now, end).toInt()
            if (daysLeft !in 0..UPCOMING_WINDOW_DAYS) null
            else DashboardUpcomingDate(contract.id, contract.title, end, daysLeft)
        }.sortedBy { it.daysLeft }.take(UPCOMING_DATE_COUNT)
    }

    private fun showContracts(items: List<EmploymentContract>) {
        _uiState.update { state ->
            state.copy(
                isLoading = false,
                errorMessageRes = null,
                totalContracts = items.size,
                analyzedContracts = items.count { it.risk != EmploymentContractRisk.NOT_ANALYZED },
                reviewContracts = items.count { it.risk == EmploymentContractRisk.HIGH || it.risk == EmploymentContractRisk.CRITICAL },
                upcomingDates = upcomingDates(items),
                recentContracts = items.sortedByDescending { it.updatedAtMillis }.take(RECENT_CONTRACT_COUNT).map { contract ->
                    DashboardRecentContract(contract.toContractListItem(""), contract.updatedAtMillis)
                }
            )
        }
    }

    private fun loadProfile() {
        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            val profile = try {
                profiles.getProfile()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                return@launch
            }
            _uiState.update { it.copy(userName = profile.name, photoUri = profile.photoUri) }
        }
    }

    private fun emitEffect(effect: HomeDashboardUiEffect) {
        viewModelScope.launch { _uiEffect.send(effect) }
    }

    class Factory(
        private val contracts: ContractRepository,
        private val profiles: ProfileRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeDashboardViewModel(contracts, profiles) as T
    }

    private companion object {
        const val RECENT_CONTRACT_COUNT = 2
        const val UPCOMING_DATE_COUNT = 3
        const val UPCOMING_WINDOW_DAYS = 90
    }
}
