package com.example.lagallens.data.datasource.local

import android.content.Context
import java.io.File

internal class DocumentSessionCache(context: Context) {
    private val directory = File(context.cacheDir, DIRECTORY)

    fun newFile(): File {
        directory.mkdirs()
        return File.createTempFile("document-", ".tmp", directory)
    }

    /** Deletes [file] only when it lives directly inside this cache directory. */
    fun delete(file: File): Boolean {
        val target = file.canonicalFile
        return target.parentFile == directory.canonicalFile && target.delete()
    }

    companion object {
        const val DIRECTORY = "document-session"
    }
}
