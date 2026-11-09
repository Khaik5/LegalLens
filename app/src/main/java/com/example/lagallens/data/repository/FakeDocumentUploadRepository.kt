package com.example.lagallens.data.repository

import com.example.lagallens.core.network.BackendNotConfiguredException
import com.example.lagallens.domain.model.processing.DocumentUploadRequest
import com.example.lagallens.domain.model.processing.ProcessingState
import com.example.lagallens.domain.repository.DocumentUploadRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** DEMO only: nothing leaves the device; the delays stand in for transfer progress. */
class FakeDocumentUploadRepository : DocumentUploadRepository {
    override fun upload(request: DocumentUploadRequest): Flow<ProcessingState> = flow {
        emit(ProcessingState.Running(progressPercent = 68))
        delay(700)
        emit(ProcessingState.Running(progressPercent = 100))
        delay(300)
        emit(ProcessingState.Completed)
    }
}

class UnconfiguredRemoteDocumentUploadRepository : DocumentUploadRepository {
    override fun upload(request: DocumentUploadRequest): Flow<ProcessingState> = flow {
        throw BackendNotConfiguredException("Upload")
    }
}
