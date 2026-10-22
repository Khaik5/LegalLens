package com.example.lagallens.domain.usecase

import com.example.lagallens.domain.model.ocr.OcrResult
import com.example.lagallens.domain.repository.OcrRepository

class GetOcrResultUseCase(
    private val repository: OcrRepository
) {
    suspend operator fun invoke(documentName: String, documentDetails: String): OcrResult =
        repository.getResult(documentName, documentDetails)
}
