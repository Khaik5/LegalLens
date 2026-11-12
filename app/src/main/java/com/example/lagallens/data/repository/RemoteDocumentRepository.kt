package com.example.lagallens.data.repository

import android.content.Context
import android.net.Uri
import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.datasource.local.DocumentSessionCache
import com.example.lagallens.data.datasource.local.datastore.AuthSessionStore
import com.example.lagallens.data.datasource.local.datastore.currentSessionScope
import com.example.lagallens.data.datasource.remote.DocumentRemoteDataSource
import com.example.lagallens.data.datasource.remote.OcrRemoteDataSource
import com.example.lagallens.data.mapper.toDocument
import com.example.lagallens.domain.model.EmploymentDocument
import com.example.lagallens.domain.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException

class RemoteDocumentRepository(
    private val context: Context,
    private val tokens: AuthSessionStore,
    private val remote: DocumentRemoteDataSource,
    private val ocr: OcrRemoteDataSource,
    private val boundSession: String? = null
) : DocumentRepository {
    override fun forSession(sessionKey: String): DocumentRepository {
        check(boundSession == null || boundSession == sessionKey) { "Session changed" }
        return RemoteDocumentRepository(context, tokens, remote, ocr, sessionKey)
    }

    override suspend fun getDocument(documentId: String): EmploymentDocument? = inSession { scope ->
        remote.get(scope, documentId)?.toDocument()
    }

    override suspend fun listDocuments(contractId: String): List<EmploymentDocument> = inSession { scope ->
        remote.list(scope, contractId).map { it.toDocument() }
    }

    override suspend fun deleteDocument(documentId: String): Boolean = inSession { scope ->
        remote.delete(scope, documentId)
    }

    override suspend fun cacheDocumentFile(documentId: String, forDownload: Boolean): String = inSession { scope ->
        val file = DocumentSessionCache(context).newFile()
        var complete = false
        try {
            val response = if (forDownload) remote.download(scope, documentId) else remote.preview(scope, documentId)
            response.use { body ->
                val input = body.byteStream()
                val written = file.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        total += count
                    }
                    total
                }
                if (written == 0L || (body.contentLength() >= 0 && written != body.contentLength())) {
                    throw IOException("Incomplete document file")
                }
            }
            currentCoroutineContext().ensureActive()
            check(tokens.currentSessionScope()?.key == scope.key) { "Session changed" }
            complete = true
            Uri.fromFile(file).toString()
        } finally {
            if (!complete) file.delete()
        }
    }

    override fun releaseDocumentFile(fileUri: String) {
        val uri = Uri.parse(fileUri)
        if (uri.scheme != "file") return
        uri.path?.let { DocumentSessionCache(context).delete(java.io.File(it)) }
    }

    /**
     * The file itself is uploaded separately, so saving only stores the reviewed text as a new OCR revision.
     * A document that has not been uploaded cannot be saved.
     */
    override suspend fun saveDocument(document: EmploymentDocument): EmploymentDocument = inSession { scope ->
        val existing = checkNotNull(remote.get(scope, document.id)) { "Document unavailable" }.toDocument()
        check(document.contractId == null || document.contractId == existing.contractId) { "Document unavailable" }
        if (document.content.isNotBlank()) check(saveRecognizedText(scope, document.id, document.content)) { "OCR result unavailable" }
        existing.copy(content = document.content, uri = document.uri)
    }

    override suspend fun updateDocumentContent(documentId: String, contractId: String?, content: String): Boolean = inSession { scope ->
        val existing = remote.get(scope, documentId) ?: return@inSession false
        if (contractId != null && existing.contractId.toString() != contractId) return@inSession false
        saveRecognizedText(scope, documentId, content)
    }

    /** False when the document has no finished OCR result to correct. Unchanged text adds no revision. */
    private suspend fun saveRecognizedText(scope: AuthSessionScope, documentId: String, text: String): Boolean {
        val latest = ocr.latest(scope, documentId)
        val revision = latest?.revision
        if (latest?.status != OCR_COMPLETED || revision == null) return false
        if (latest.text == text) return true
        ocr.correct(scope, documentId, revision, text)
        return true
    }

    private suspend fun <T> inSession(action: suspend (AuthSessionScope) -> T): T = withContext(Dispatchers.IO) {
        val scope = checkNotNull(tokens.currentSessionScope()) { "No active session" }
        check(boundSession == null || boundSession == scope.key) { "Session changed" }
        val result = action(scope)
        check(tokens.currentSessionScope()?.key == scope.key) { "Session changed" }
        result
    }

    private companion object {
        const val OCR_COMPLETED = "COMPLETED"
    }
}
