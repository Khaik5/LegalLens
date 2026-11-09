package com.example.lagallens.domain.repository

import com.example.lagallens.domain.model.processing.DocumentUploadRequest
import com.example.lagallens.domain.model.processing.ProcessingState
import kotlinx.coroutines.flow.Flow

interface DocumentUploadRepository {
    /** Cancelling collection cancels the upload. */
    fun upload(request: DocumentUploadRequest): Flow<ProcessingState>
}
