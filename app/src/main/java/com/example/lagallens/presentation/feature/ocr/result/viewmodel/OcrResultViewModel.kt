package com.example.lagallens.presentation.feature.ocr.result.viewmodel

import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.core.network.ApiFailureKind
import com.example.lagallens.core.network.ApiFailureMapper
import com.example.lagallens.domain.model.EmploymentContract
import com.example.lagallens.domain.model.EmploymentContractRisk
import com.example.lagallens.domain.model.EmploymentContractStatus
import com.example.lagallens.domain.model.EmploymentDocument
import com.example.lagallens.domain.model.ocr.OcrExtraction
import com.example.lagallens.domain.model.ocr.OcrResultNotReadyException
import com.example.lagallens.domain.usecase.GetOcrResultUseCase
import com.example.lagallens.domain.repository.ContractRepository
import com.example.lagallens.domain.repository.DocumentRepository
import com.example.lagallens.presentation.feature.ocr.result.contract.OcrResultUiEffect
import com.example.lagallens.presentation.feature.ocr.result.contract.OcrResultUiEvent
import com.example.lagallens.presentation.feature.ocr.result.contract.OcrResultUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import com.example.lagallens.presentation.common.utils.toFailureMessage

class OcrResultViewModel(
    private val getOcrResult: GetOcrResultUseCase,
    private val documentRepository: DocumentRepository,
    private val contractRepository: ContractRepository,
    private val defaultContractTitle: String = "",
    private val unsavedUploads: com.example.lagallens.domain.model.UnsavedUploadContracts = com.example.lagallens.domain.model.UnsavedUploadContracts(),
    private val isOwner: () -> Boolean = { true }
) : ViewModel() {
    private val _uiState = MutableStateFlow(OcrResultUiState())
    val uiState = _uiState.asStateFlow()
    private val _uiEffect = Channel<OcrResultUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun onEvent(event: OcrResultUiEvent) {
        if (!isOwner()) { _uiState.value = OcrResultUiState(); return }
        when (event) {
            is OcrResultUiEvent.ScreenOpened -> loadResult(event)
            OcrResultUiEvent.BackClicked -> {
                discardEmptyUpload()
                emitEffect(OcrResultUiEffect.NavigateBack)
            }
            OcrResultUiEvent.EditClicked -> emitEffect(OcrResultUiEffect.NavigateToEdit(_uiState.value.documentText))
            OcrResultUiEvent.ReviewClicked -> emitEffect(OcrResultUiEffect.NavigateToReview)
            is OcrResultUiEvent.DocumentUpdated -> {
                _uiState.update { it.copy(documentText = event.documentText) }
                persistEditedText(event.documentText)
            }
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
                check(isOwner())
                if (!event.contractId.isNullOrBlank()) {
                    val contract = contractRepository.getContract(event.contractId)
                    check(isOwner() && contract != null && contract.documentId == documentId && persisted?.contractId == event.contractId) { "Document unavailable" }
                }
                // An uploaded document without saved text still needs its recognized text.
                val ocrResult = if (persisted?.content.isNullOrBlank() && event.initialContent.isBlank()) getOcrResult(documentId) else null
                check(isOwner())
                val text = persisted?.content?.takeIf(String::isNotBlank)
                    ?: event.initialContent.takeIf(String::isNotBlank)
                    ?: ocrResult?.documentText.orEmpty()
                val savedDocument = documentRepository.saveDocument(
                    EmploymentDocument(
                        id = documentId,
                        contractId = event.contractId ?: persisted?.contractId,
                        displayName = event.documentName,
                        uri = event.documentUri ?: persisted?.uri,
                        mimeType = persisted?.mimeType?.takeIf(String::isNotBlank) ?: event.mimeType,
                        content = text,
                        updatedAtMillis = System.currentTimeMillis()
                    )
                )
                check(isOwner())
                _uiState.update {
                    it.copy(
                        documentText = text,
                        isTextLoaded = true,
                        documentUri = savedDocument.uri,
                        mimeType = savedDocument.mimeType,
                        warnings = ocrResult?.warnings.orEmpty(),
                        isSampleContent = ocrResult?.isSampleContent == true,
                        confidenceLabel = ocrResult?.confidenceLabel.orEmpty(),
                        // A text-layer PDF has no recognition confidence; the banner then says so instead of
                        // showing an empty label next to "—".
                        confidenceLabelRes = if (ocrResult == null) R.string.ocr_result_saved_content_label
                            else if (ocrResult.extraction == OcrExtraction.TEXT_LAYER) R.string.ocr_result_extraction_text_layer
                            else ocrResult.confidence?.let(::confidenceLabelRes)
                                ?: R.string.ocr_result_confidence_unavailable.takeIf { ocrResult.confidenceLabel.isNullOrBlank() },
                        confidencePercentage = if (ocrResult?.extraction == OcrExtraction.TEXT_LAYER) "—" else ocrResult?.confidencePercentage ?: "—",
                        errorMessageRes = null
                    )
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: OcrResultNotReadyException) {
                // The document has no text yet: recognize it first, then this screen opens again.
                if (!isOwner()) { _uiState.value = OcrResultUiState(); return@launch }
                emitEffect(OcrResultUiEffect.NavigateToOcrProcessing(event.documentName, event.documentDetails, documentId, event.contractId, event.mimeType))
            } catch (failure: Exception) {
                if (!isOwner()) { _uiState.value = OcrResultUiState(); return@launch }
                _uiState.update { it.copy(confidenceLabel = "", confidenceLabelRes = null, confidencePercentage = "", documentText = "", errorMessageRes = failure.toFailureMessage()) }
            }
        }
    }

    /** An edit survives leaving the screen or process death, before the review is confirmed. */
    private fun persistEditedText(text: String) {
        val current = _uiState.value
        viewModelScope.launch {
            try {
                check(documentRepository.updateDocumentContent(current.documentId, current.contractId, text)) { "Document unavailable" }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (failure: Exception) {
                if (isOwner()) _uiState.update {
                    it.copy(errorMessageRes = if (failure.isEditConflict()) R.string.ocr_edit_conflict else R.string.ocr_edit_save_failed)
                }
            }
        }
    }

    /** Leaving with no recognized text removes the contract this upload created, never any other contract. */
    private fun discardEmptyUpload() {
        val current = _uiState.value
        val contractId = current.contractId?.takeIf(String::isNotBlank) ?: return
        if (!current.isTextLoaded || current.documentText.isNotBlank() || current.errorMessageRes != null) return
        if (!unsavedUploads.takeIfUnsaved(contractId)) return
        viewModelScope.launch {
            // The screen is closing, so the removal must finish even after this scope is cancelled.
            withContext(NonCancellable) {
                try {
                    contractRepository.deleteContract(contractId)
                } catch (_: Exception) {
                    android.util.Log.w("OcrResult", "Could not remove the empty upload contract")
                }
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
                val document = EmploymentDocument(current.documentId, contractId, current.documentName, current.documentUri, current.mimeType, content, timestamp)
                val existing = if (current.contractId.isNullOrBlank()) null else contractRepository.getContract(current.contractId)
                check(isOwner())
                if (existing == null && !current.contractId.isNullOrBlank()) error("Contract unavailable")
                if (existing != null) check(existing.documentId == current.documentId) { "Document unavailable" }
                if (existing == null) {
                    // A document can only point at an existing contract: save it unlinked, create the contract, then link it.
                    val saved = documentRepository.saveDocument(document.copy(contractId = null))
                    check(isOwner())
                    contractRepository.createContract(
                        EmploymentContract(
                            id = contractId,
                            documentId = current.documentId,
                            title = current.documentName.ifBlank { defaultContractTitle },
                            content = content,
                            createdAtMillis = timestamp,
                            updatedAtMillis = timestamp,
                            status = if (current.isSampleContent) EmploymentContractStatus.DRAFT else EmploymentContractStatus.COMPLETE,
                            risk = EmploymentContractRisk.NOT_ANALYZED,
                            fileName = current.documentName,
                            fileUri = saved.uri ?: current.documentUri,
                            mimeType = current.mimeType
                        )
                    )
                    check(isOwner())
                    check(documentRepository.updateDocumentContent(current.documentId, contractId, content)) { "Document unavailable" }
                } else {
                    documentRepository.saveDocument(document)
                    check(isOwner())
                    contractRepository.updateContract(existing.copy(content = content, updatedAtMillis = timestamp))
                }
                check(isOwner())
                unsavedUploads.markSaved(contractId)
                _uiState.update { it.copy(contractId = contractId, isReviewConfirmed = true) }
                emitEffect(OcrResultUiEffect.NavigateToSaved(current.documentName, current.documentId, contractId))
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (failure: Exception) {
                if (!isOwner()) { _uiState.value = OcrResultUiState(); return@launch }
                _uiState.update {
                    it.copy(errorMessageRes = if (failure.isEditConflict()) R.string.ocr_edit_conflict else failure.toFailureMessage())
                }
            }
        }
    }

    /** The text was saved from another screen or device after this one loaded it. */
    private fun Exception.isEditConflict(): Boolean = ApiFailureMapper.map(this).kind == ApiFailureKind.CONFLICT

    private fun confidenceLabelRes(confidence: Double): Int = when {
        confidence >= HIGH_CONFIDENCE -> R.string.ocr_result_confidence_high
        confidence >= MEDIUM_CONFIDENCE -> R.string.ocr_result_confidence_medium
        else -> R.string.ocr_result_confidence_low
    }

    private fun emitEffect(effect: OcrResultUiEffect) {
        viewModelScope.launch { if (isOwner()) _uiEffect.send(effect) }
    }

    class Factory(
        private val getOcrResult: GetOcrResultUseCase,
        private val documentRepository: DocumentRepository,
        private val contractRepository: ContractRepository,
        private val unsavedUploads: com.example.lagallens.domain.model.UnsavedUploadContracts = com.example.lagallens.domain.model.UnsavedUploadContracts(),
        private val isOwner: () -> Boolean = { true }
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            require(modelClass.isAssignableFrom(OcrResultViewModel::class.java))
            val application = requireNotNull(extras[APPLICATION_KEY])
            val context = AppCompatDelegate.getApplicationLocales()[0]?.let { locale ->
                application.createConfigurationContext(Configuration(application.resources.configuration).apply { setLocale(locale) })
            } ?: application
            return OcrResultViewModel(
                getOcrResult, documentRepository, contractRepository,
                context.getString(R.string.ocr_result_default_contract_name), unsavedUploads, isOwner
            ) as T
        }
    }

    private companion object {
        const val HIGH_CONFIDENCE = 0.9
        const val MEDIUM_CONFIDENCE = 0.7
    }
}
