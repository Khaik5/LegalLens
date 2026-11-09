package com.example.lagallens.domain.repository

import com.example.lagallens.domain.model.ocr.OcrResult
import com.example.lagallens.domain.model.processing.ProcessingState
import kotlinx.coroutines.flow.Flow

interface OcrRepository {
    fun process(documentId: String): Flow<ProcessingState>
    suspend fun getResult(documentId: String): OcrResult
}
