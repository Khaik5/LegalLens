package com.example.lagallens.presentation.feature.ocr.result.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.domain.model.EmploymentContract
import com.example.lagallens.domain.model.EmploymentContractRisk
import com.example.lagallens.domain.model.EmploymentContractStatus
import com.example.lagallens.domain.model.EmploymentDocument
import com.example.lagallens.domain.usecase.GetOcrResultUseCase
import com.example.lagallens.domain.repository.ContractRepository
import com.example.lagallens.domain.repository.DocumentRepository
import com.example.lagallens.presentation.feature.ocr.result.contract.OcrResultUiEffect
import com.example.lagallens.presentation.feature.ocr.result.contract.OcrResultUiEvent
import com.example.lagallens.presentation.feature.ocr.result.contract.OcrResultUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class OcrResultViewModel(
    private val getOcrResult: GetOcrResultUseCase,
    private val documentRepository: DocumentRepository,
    private val contractRepository: ContractRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(OcrResultUiState())
    val uiState = _uiState.asStateFlow()
    private val _uiEffect = MutableSharedFlow<OcrResultUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: OcrResultUiEvent) {
        when (event) {
            is OcrResultUiEvent.ScreenOpened -> loadResult(event)
            OcrResultUiEvent.BackClicked -> emitEffect(OcrResultUiEffect.NavigateBack)
            OcrResultUiEvent.EditClicked -> emitEffect(OcrResultUiEffect.NavigateToEdit(_uiState.value.documentText))
            OcrResultUiEvent.ReviewClicked -> emitEffect(OcrResultUiEffect.NavigateToReview)
            is OcrResultUiEvent.DocumentUpdated -> _uiState.update { it.copy(documentText = event.documentText) }
            is OcrResultUiEvent.ReviewConfirmed -> {
                val current = _uiState.value
                if (event.documentId != null && event.documentId != current.documentId) {
                    _uiState.update { it.copy(errorMessageRes = R.string.auth_backend_not_configured) }
                } else if (event.contractId != null && event.contractId != current.contractId) {
                    _uiState.update { it.copy(errorMessageRes = R.string.auth_backend_not_configured) }
                } else persistAndSave(event.documentText)
            }
        }
    }

    private fun loadResult(event: OcrResultUiEvent.ScreenOpened) {
        val documentId = event.documentId.ifBlank { UUID.randomUUID().toString() }
        if (_uiState.value.documentId == documentId && _uiState.value.documentText.isNotBlank()) return
        _uiState.update { it.copy(documentId = documentId, contractId = event.contractId, documentUri = event.documentUri, documentName = event.documentName, documentDetails = event.documentDetails, errorMessageRes = null) }
        viewModelScope.launch {
            try {
                val persisted = documentRepository.getDocument(documentId)
                val ocrResult = if (persisted == null && event.initialContent.isBlank()) getOcrResult(event.documentName, event.documentDetails) else null
                val text = persisted?.content?.takeIf(String::isNotBlank)
                    ?: event.initialContent.takeIf(String::isNotBlank)
                    ?: ocrResult?.documentText.orEmpty()
                documentRepository.saveDocument(
                    EmploymentDocument(
                        id = documentId,
                        contractId = event.contractId ?: persisted?.contractId,
                        displayName = event.documentName,
                        uri = event.documentUri ?: persisted?.uri,
                        mimeType = persisted?.mimeType.orEmpty(),
                        content = text,
                        updatedAtMillis = System.currentTimeMillis()
                    )
                )
                _uiState.update {
                    it.copy(
                        documentText = text,
                        confidenceLabel = ocrResult?.confidenceLabel ?: "DEMO · Nội dung đã lưu",
                        confidencePercentage = ocrResult?.confidencePercentage ?: "—",
                        errorMessageRes = null
                    )
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Exception) {
                _uiState.update { it.copy(confidenceLabel = "", confidencePercentage = "", documentText = "", errorMessageRes = R.string.auth_backend_not_configured) }
            }
        }
    }

    private fun persistAndSave(editedText: String?) {
        val current = _uiState.value
        val content = editedText ?: current.documentText
        if (content.isBlank() || current.documentId.isBlank()) {
            _uiState.update { it.copy(errorMessageRes = R.string.ocr_review_empty_document) }
            return
        }
        _uiState.update { it.copy(documentText = content, errorMessageRes = null) }
        viewModelScope.launch {
            try {
                val timestamp = System.currentTimeMillis()
                val contractId = current.contractId ?: UUID.randomUUID().toString()
                val document = EmploymentDocument(current.documentId, contractId, current.documentName, current.documentUri, "", content, timestamp)
                documentRepository.saveDocument(document)
                val existing = if (current.contractId.isNullOrBlank()) null else contractRepository.getContract(current.contractId)
                if (existing == null) {
                    contractRepository.createContract(
                        EmploymentContract(
                            id = contractId,
                            documentId = current.documentId,
                            title = current.documentName.ifBlank { "Hợp đồng lao động mới" },
                            content = content,
                            createdAtMillis = timestamp,
                            updatedAtMillis = timestamp,
                            status = EmploymentContractStatus.COMPLETE,
                            risk = EmploymentContractRisk.NOT_ANALYZED,
                            fileName = current.documentName,
                            fileUri = current.documentUri,
                            mimeType = ""
                        )
                    )
                } else {
                    contractRepository.updateContract(existing.copy(content = content, updatedAtMillis = timestamp))
                }
                _uiState.update { it.copy(contractId = contractId, isReviewConfirmed = true) }
                emitEffect(OcrResultUiEffect.NavigateToSaved(current.documentName, current.documentId, contractId))
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessageRes = R.string.auth_backend_not_configured) }
            }
        }
    }

    private fun emitEffect(effect: OcrResultUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }

    class Factory(
        private val getOcrResult: GetOcrResultUseCase,
        private val documentRepository: DocumentRepository,
        private val contractRepository: ContractRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(OcrResultViewModel::class.java))
            return OcrResultViewModel(getOcrResult, documentRepository, contractRepository) as T
        }
    }
}
