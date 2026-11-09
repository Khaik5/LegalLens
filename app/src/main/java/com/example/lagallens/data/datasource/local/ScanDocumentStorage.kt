package com.example.lagallens.data.datasource.local

import android.content.Context
import android.net.Uri
import com.example.lagallens.data.datasource.local.datastore.AccountStorageName
import com.example.lagallens.domain.model.EmploymentDocument
import java.io.File

internal class ScanDocumentStorage(private val context: Context) {
    fun retain(document: EmploymentDocument, userId: String): EmploymentDocument {
        val uri = document.uri?.let(Uri::parse) ?: return document
        if (uri.scheme != "file") return document
        val source = File(uri.path ?: return document).canonicalFile
        val scanRoot = File(context.cacheDir, "scan-session").canonicalFile
        if (!source.toPath().startsWith(scanRoot.toPath())) return document
        check(source.isFile) { "Scanned image is unavailable" }
        val directory = File(context.filesDir, "scan-documents/${AccountStorageName.forUser("owner", userId)}")
        check(directory.isDirectory || directory.mkdirs()) { "Cannot retain scan" }
        val name = java.util.UUID.nameUUIDFromBytes(document.id.toByteArray(Charsets.UTF_8)).toString() + ".jpg"
        val target = android.util.AtomicFile(File(directory, name))
        val output = target.startWrite()
        try {
            source.inputStream().use { it.copyTo(output) }
            target.finishWrite(output)
        } catch (failure: Throwable) {
            target.failWrite(output)
            throw failure
        }
        return document.copy(uri = Uri.fromFile(target.baseFile).toString(), mimeType = document.mimeType.ifBlank { "image/jpeg" })
    }
}
