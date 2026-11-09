package com.example.lagallens.presentation.feature.creditstore.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelProvider
import com.example.lagallens.domain.repository.CreditRepository
import com.example.lagallens.domain.repository.PaymentRepository
import com.example.lagallens.presentation.feature.creditstore.contract.CreditPackage
import kotlinx.coroutines.CancellationException
import com.example.lagallens.presentation.feature.creditstore.contract.CreditStoreUiEffect
import com.example.lagallens.presentation.feature.creditstore.contract.CreditStoreUiEvent
import com.example.lagallens.presentation.feature.creditstore.contract.CreditStoreUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreditStoreViewModel(
    private val creditRepository: CreditRepository,
    private val paymentRepository: PaymentRepository
) : ViewModel() {
    private val ownerSession = creditRepository.activeSessionKey()
    fun isOwnerSession() = ownerSession == creditRepository.activeSessionKey()
    private val _uiState = MutableStateFlow(CreditStoreUiState(
        balanceCredits = creditRepository.balance.value,
        isBackendUnavailable = !creditRepository.isConfigured
    ))
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<CreditStoreUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    init {
        viewModelScope.launch {
            try {
                val offers = paymentRepository.getPackages()
                if (isOwnerSession()) _uiState.update { state ->
                    state.copy(isLoading = false, packages = offers.map { offer ->
                        CreditPackage(offer.id, offer.credits, offer.priceVnd, offer.title, offer.description, offer.isRecommended)
                    })
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, isBackendUnavailable = true) }
            }
        }
    }
    init { viewModelScope.launch { creditRepository.balance.collect { balance -> _uiState.update { it.copy(balanceCredits = if (isOwnerSession()) balance else 0) } } } }
    init { viewModelScope.launch { creditRepository.observeSessionKeys().collect { if (it != ownerSession) _uiState.value = CreditStoreUiState(balanceCredits = 0, isBackendUnavailable = !creditRepository.isConfigured) } } }

    fun onEvent(event: CreditStoreUiEvent) {
        if (!isOwnerSession()) return
        when (event) {
            CreditStoreUiEvent.BackClicked -> emitEffect(CreditStoreUiEffect.NavigateBack)
            CreditStoreUiEvent.TopUpClicked -> if (!creditRepository.isConfigured) {
                emitEffect(CreditStoreUiEffect.ShowBackendUnavailable)
            } else _uiState.value.let { state ->
                state.packages.firstOrNull { it.isSelected }?.let { selected ->
                    emitEffect(CreditStoreUiEffect.NavigateToPurchaseConfirmation(selected, state.balanceCredits))
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
            if (isOwnerSession()) _uiEffect.send(effect)
        }
    }

    class Factory(private val repository: CreditRepository, private val payments: PaymentRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = CreditStoreViewModel(repository, payments) as T
    }
}
