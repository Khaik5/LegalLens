package com.example.lagallens.presentation.feature.creditstore.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelProvider
import com.example.lagallens.domain.repository.CreditRepository
import com.example.lagallens.presentation.feature.creditstore.contract.CreditStoreUiEffect
import com.example.lagallens.presentation.feature.creditstore.contract.CreditStoreUiEvent
import com.example.lagallens.presentation.feature.creditstore.contract.CreditStoreUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreditStoreViewModel(private val creditRepository: CreditRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(CreditStoreUiState(
        balanceCredits = creditRepository.balance.value,
        isBackendUnavailable = !creditRepository.isConfigured
    ))
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<CreditStoreUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    init { viewModelScope.launch { creditRepository.balance.collect { balance -> _uiState.update { it.copy(balanceCredits = balance) } } } }

    fun onEvent(event: CreditStoreUiEvent) {
        when (event) {
            CreditStoreUiEvent.BackClicked -> emitEffect(CreditStoreUiEffect.NavigateBack)
            CreditStoreUiEvent.TopUpClicked -> if (!creditRepository.isConfigured) {
                emitEffect(CreditStoreUiEffect.ShowBackendUnavailable)
            } else _uiState.value.let { state ->
                state.packages.firstOrNull { it.isSelected }?.let { selected ->
                    emitEffect(CreditStoreUiEffect.NavigateToPurchaseConfirmation(selected.id, state.balanceCredits))
                }
            }
            CreditStoreUiEvent.ViewHistoryClicked -> emitEffect(CreditStoreUiEffect.NavigateToCreditHistory)
            is CreditStoreUiEvent.PackageClicked -> _uiState.update { state ->
                if (state.packages.none { it.id == event.packageId }) {
                    state
                } else {
                    state.copy(
                        packages = state.packages.map { creditPackage ->
                            creditPackage.copy(isSelected = creditPackage.id == event.packageId)
                        }
                    )
                }
            }
        }
    }

    private fun emitEffect(effect: CreditStoreUiEffect) {
        viewModelScope.launch {
            _uiEffect.emit(effect)
        }
    }

    class Factory(private val repository: CreditRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = CreditStoreViewModel(repository) as T
    }
}
