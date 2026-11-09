package com.example.lagallens.domain.usecase

import com.example.lagallens.domain.model.ocr.OcrResult
import com.example.lagallens.domain.repository.OcrRepository

class GetOcrResultUseCase(
    private val repository: OcrRepository
) {
    suspend operator fun invoke(documentId: String): OcrResult = repository.getResult(documentId)
}
