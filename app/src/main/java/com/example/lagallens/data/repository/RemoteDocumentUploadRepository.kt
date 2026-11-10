package com.example.lagallens.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.core.network.DocumentFiles
import com.example.lagallens.data.datasource.local.DocumentSessionCache
import com.example.lagallens.data.datasource.local.datastore.AuthSessionStore
import com.example.lagallens.data.datasource.local.datastore.currentSessionScope
import com.example.lagallens.data.datasource.local.writeScanPdf
import com.example.lagallens.data.datasource.remote.DocumentRemoteDataSource
import com.example.lagallens.data.datasource.remote.UploadProgressRequestBody
import com.example.lagallens.domain.model.EmploymentContract
import com.example.lagallens.domain.model.processing.DocumentUploadRequest
import com.example.lagallens.domain.model.processing.ProcessingState
import com.example.lagallens.domain.model.processing.UploadFailure
import com.example.lagallens.domain.repository.ContractRepository
import com.example.lagallens.domain.repository.DocumentUploadRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.HttpException
import java.io.File
import java.io.IOException

class RemoteDocumentUploadRepository(
    private val context: Context,
    private val tokens: AuthSessionStore,
    private val contracts: ContractRepository,
    private val documents: DocumentRemoteDataSource
) : DocumentUploadRepository {
    override fun upload(request: DocumentUploadRequest): Flow<ProcessingState> = channelFlow {
        val scope = tokens.currentSessionScope()
        if (scope == null || (request.sessionKey != null && request.sessionKey != scope.key)) {
            send(ProcessingState.Failed())
            return@channelFlow
        }
        val boundContracts = contracts.forSession(scope.key)
        var stagingFile: File? = null
        var createdContractId: String? = null
        var uploaded = false
        var contactingBackend = false
        try {
            send(ProcessingState.Running(progressPercent = 0))
            val mimeType = if (request.isScan) DocumentFiles.PDF else request.mimeType
            if (!DocumentFiles.isSupported(mimeType)) throw RejectedFile(UploadFailure.UNSUPPORTED_FORMAT)
            val file = DocumentSessionCache(context).newFile()
            stagingFile = file
            if (request.isScan) {
                require(request.sourceUris.isNotEmpty())
                writeScanPdf(context.contentResolver, request.sourceUris, file, DocumentFiles.MAX_UPLOAD_BYTES)
            } else {
                stageFile(Uri.parse(request.sourceUris.single()), file)
            }
            if (file.length() > DocumentFiles.MAX_UPLOAD_BYTES) throw RejectedFile(UploadFailure.TOO_LARGE)
            if (file.length() == 0L) throw RejectedFile(UploadFailure.UNREADABLE)

            val title = request.displayName.substringBeforeLast('.').trim()
                .ifBlank { request.displayName.trim() }.take(255)
            val now = System.currentTimeMillis()
            contactingBackend = true
            currentCoroutineContext().ensureActive()
            // Keep the returned ID even if the user cancels while the backend is creating the contract.
            withContext(NonCancellable) {
                createdContractId = boundContracts.createContract(
                    EmploymentContract("", "", title, "", now, now)
                ).id
            }

            val contractId = checkNotNull(createdContractId)
            val body = UploadProgressRequestBody(file, mimeType.toMediaType()) { percent ->
                trySend(ProcessingState.Running(progressPercent = percent))
            }
            val document = documents.upload(AuthSessionScope(scope.key), contractId, request.displayName, body)
            uploaded = true
            // The file is safely stored; a session switch only means this screen no longer belongs to that user.
            if (tokens.currentSessionScope()?.key != scope.key) return@channelFlow
            send(ProcessingState.Running(progressPercent = 100))
            send(ProcessingState.Uploaded(contractId, document.id.toString()))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            val reason = when (error) {
                is RejectedFile -> error.reason
                is HttpException -> {
                    error.response()?.errorBody()?.close()
                    when {
                        error.code() == 413 && createdContractId != null -> UploadFailure.TOO_LARGE
                        error.code() == 400 && createdContractId != null -> UploadFailure.UNSUPPORTED_FORMAT
                        else -> UploadFailure.UNKNOWN
                    }
                }
                is IOException -> if (contactingBackend) UploadFailure.NETWORK else UploadFailure.UNREADABLE
                else -> UploadFailure.UNKNOWN
            }
            rollback(boundContracts, createdContractId, uploaded)
            createdContractId = null
            send(ProcessingState.Failed(
                isRetryable = reason != UploadFailure.TOO_LARGE && reason != UploadFailure.UNSUPPORTED_FORMAT,
                reason = reason
            ))
        } finally {
            stagingFile?.delete()
            rollback(boundContracts, createdContractId, uploaded)
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun stageFile(uri: Uri, target: File) {
        val input = context.contentResolver.openInputStream(uri) ?: throw RejectedFile(UploadFailure.UNREADABLE)
        input.use { source ->
            target.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0L
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val count = source.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > DocumentFiles.MAX_UPLOAD_BYTES) throw RejectedFile(UploadFailure.TOO_LARGE)
                    output.write(buffer, 0, count)
                }
            }
        }
    }

    private suspend fun rollback(contracts: ContractRepository, contractId: String?, uploaded: Boolean) {
        if (contractId == null || uploaded) return
        withContext(NonCancellable) {
            try {
                contracts.deleteContract(contractId)
            } catch (_: Exception) {
                Log.w("DocumentUpload", "Could not remove incomplete contract")
            }
        }
    }

    private class RejectedFile(val reason: UploadFailure) : IOException()
}
