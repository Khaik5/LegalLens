package com.example.lagallens.data.datasource.local

import android.content.Context
import java.io.File

/** Removes contract images and PDFs left in the shared cache when a session ends. */
class SessionCacheCleaner(private val cacheDir: File) {
    constructor(context: Context) : this(context.applicationContext.cacheDir)

    /**
     * Deletes files written before [createdBefore]. Files the next session writes after the change are kept,
     * so a cleanup that runs late cannot remove a scan the new user just took.
     */
    fun clear(createdBefore: Long = Long.MAX_VALUE) {
        SESSION_DIRECTORIES.map { File(cacheDir, it) }.filter { it.exists() }.forEach { root ->
            root.walkBottomUp().forEach { file ->
                when {
                    file.isFile && file.lastModified() < createdBefore -> file.delete()
                    file.isDirectory && file != root && file.list().isNullOrEmpty() -> file.delete()
                }
            }
        }
    }

    private companion object {
        val SESSION_DIRECTORIES = listOf(
            "scan-session",
            "draft-pdfs",
            DocumentSessionCache.DIRECTORY,
            ProfileAvatarFiles.DIRECTORY
        )
    }
}
