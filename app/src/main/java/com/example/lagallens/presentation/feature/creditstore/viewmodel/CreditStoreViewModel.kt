package com.example.lagallens.presentation.feature.creditstore.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.creditstore.contract.CreditStoreUiEffect
import com.example.lagallens.presentation.feature.creditstore.contract.CreditStoreUiEvent
import com.example.lagallens.presentation.feature.creditstore.contract.CreditStoreUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreditStoreViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CreditStoreUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<CreditStoreUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: CreditStoreUiEvent) {
        when (event) {
            CreditStoreUiEvent.BackClicked -> emitEffect(CreditStoreUiEffect.NavigateBack)
            CreditStoreUiEvent.TopUpClicked -> emitEffect(CreditStoreUiEffect.NavigateToPurchaseConfirmation)
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
}
