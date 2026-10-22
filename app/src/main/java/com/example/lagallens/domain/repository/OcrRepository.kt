package com.example.lagallens.domain.repository

import com.example.lagallens.domain.model.ocr.OcrResult

interface OcrRepository {
    suspend fun getResult(documentName: String, documentDetails: String): OcrResult
}
