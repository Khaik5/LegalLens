package com.example.lagallens.data.datasource.local

import android.content.Context
import com.google.gson.Gson
import java.io.File
import java.security.MessageDigest

/**
 * Keeps the OCR text a user is editing, so that it survives the process being killed while the editor is open.
 *
 * The text of a contract is private, so the files live in the app's own cache (excluded from backups, never
 * shared), are named by a hash of the session and the document rather than by any readable id, and are removed
 * with the other session caches when the session ends ([SessionCacheCleaner]). A draft is only returned to the
 * session that wrote it.
 */
class OcrEditDraftStore(private val cacheDir: File) {
    constructor(context: Context) : this(context.applicationContext.cacheDir)

    /** [original] is the recognised text the editing started from, [edited] what the user has made of it. */
    data class Draft(val original: String, val edited: String)

    private val gson = Gson()

    fun save(purpose: String, sessionKey: String, documentId: String, draft: Draft): Boolean = runCatching {
        val file = fileFor(purpose, sessionKey, documentId)
        file.parentFile?.mkdirs()
        // Written beside the target and renamed, so a process killed mid-write never leaves half a draft.
        val temporary = File(file.parentFile, file.name + ".tmp")
        temporary.writeText(gson.toJson(draft), Charsets.UTF_8)
        if (file.exists()) file.delete()
        temporary.renameTo(file)
    }.getOrDefault(false)

    fun load(purpose: String, sessionKey: String, documentId: String): Draft? = runCatching {
        val file = fileFor(purpose, sessionKey, documentId)
        if (!file.isFile) return null
        gson.fromJson(file.readText(Charsets.UTF_8), Draft::class.java)
            ?.takeIf { it.original != null && it.edited != null }
    }.getOrNull()

    fun clear(purpose: String, sessionKey: String, documentId: String) {
        runCatching { fileFor(purpose, sessionKey, documentId).delete() }
    }

    private fun fileFor(purpose: String, sessionKey: String, documentId: String): File {
        val digest = MessageDigest.getInstance("SHA-256").digest("$purpose|$sessionKey|$documentId".toByteArray(Charsets.UTF_8))
        val name = digest.joinToString("") { "%02x".format(it.toInt() and 0xff) }.take(40)
        return File(File(cacheDir, DIRECTORY), "$name.json")
    }

    companion object {
        const val DIRECTORY = "ocr-edit-drafts"
        const val PURPOSE_EDITOR = "editor"
        const val PURPOSE_REVIEW = "review"
    }
}
