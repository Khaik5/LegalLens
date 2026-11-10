package com.example.lagallens.data.repository

import android.content.Context
import android.net.Uri
import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.core.network.BackendNotConfiguredException
import com.example.lagallens.data.datasource.local.DocumentSessionCache
import com.example.lagallens.data.datasource.local.datastore.AuthSessionStore
import com.example.lagallens.data.datasource.local.datastore.currentSessionScope
import com.example.lagallens.data.datasource.remote.DocumentRemoteDataSource
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
    private val boundSession: String? = null
) : DocumentRepository {
    override fun forSession(sessionKey: String): DocumentRepository {
        check(boundSession == null || boundSession == sessionKey) { "Session changed" }
        return RemoteDocumentRepository(context, tokens, remote, sessionKey)
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

    override suspend fun saveDocument(document: EmploymentDocument): EmploymentDocument =
        throw BackendNotConfiguredException("Document content")

    override suspend fun updateDocumentContent(documentId: String, contractId: String?, content: String): Boolean =
        throw BackendNotConfiguredException("OCR document content")

    private suspend fun <T> inSession(action: suspend (AuthSessionScope) -> T): T = withContext(Dispatchers.IO) {
        val scope = checkNotNull(tokens.currentSessionScope()) { "No active session" }
        check(boundSession == null || boundSession == scope.key) { "Session changed" }
        val result = action(scope)
        check(tokens.currentSessionScope()?.key == scope.key) { "Session changed" }
        result
    }
}
