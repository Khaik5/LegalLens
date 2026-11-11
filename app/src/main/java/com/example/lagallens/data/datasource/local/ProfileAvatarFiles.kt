package com.example.lagallens.data.datasource.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.net.toUri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest

/** Prepares avatar uploads and keeps the signed-in account's avatar as a session cache file. */
internal interface ProfileAvatarStore {
    /** Returns the picked image as a JPEG the backend accepts, or null when it can no longer be read. */
    fun encodeForUpload(uri: String): ByteArray?

    /** Returns the cached avatar for this account and server version, if it is still on disk. */
    fun cached(userId: String, version: String): String?

    /** Replaces the account's cached avatar and returns its file URI. */
    fun save(userId: String, version: String, input: InputStream): String

    fun clear(userId: String)
}

internal class ProfileAvatarFiles(context: Context) : ProfileAvatarStore {
    private val resolver = context.applicationContext.contentResolver
    private val directory = File(context.applicationContext.cacheDir, DIRECTORY)

    override fun encodeForUpload(uri: String): ByteArray? = try {
        // ImageDecoder applies the EXIF orientation of camera photos.
        val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri.toUri())) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longest = maxOf(info.size.width, info.size.height)
            if (longest > MAX_EDGE_PX) {
                val scale = MAX_EDGE_PX.toFloat() / longest
                decoder.setTargetSize(
                    (info.size.width * scale).toInt().coerceAtLeast(1),
                    (info.size.height * scale).toInt().coerceAtLeast(1)
                )
            }
        }
        try {
            ByteArrayOutputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output))
                output.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    }

    override fun cached(userId: String, version: String): String? =
        fileFor(userId, version).takeIf { it.isFile && it.length() > 0 }?.let { Uri.fromFile(it).toString() }

    override fun save(userId: String, version: String, input: InputStream): String {
        directory.mkdirs()
        val target = fileFor(userId, version)
        val partial = File.createTempFile("avatar-", ".tmp", directory)
        try {
            partial.outputStream().use { input.copyTo(it) }
            clear(userId)
            check(partial.renameTo(target)) { "Could not store the avatar" }
        } finally {
            partial.delete()
        }
        return Uri.fromFile(target).toString()
    }

    override fun clear(userId: String) {
        val prefix = "${accountKey(userId)}-"
        directory.listFiles { file -> file.name.startsWith(prefix) }?.forEach(File::delete)
    }

    /** The version is part of the name, so an ImageView reloads after the avatar changes. */
    private fun fileFor(userId: String, version: String) =
        File(directory, "${accountKey(userId)}-${hash(version).take(16)}.jpg")

    private fun accountKey(userId: String) = requireNotNull(userId.toLongOrNull()?.takeIf { it > 0 }).toString()

    private fun hash(value: String) = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { "%02x".format(it) }

    companion object {
        const val DIRECTORY = "profile-avatar"
        private const val MAX_EDGE_PX = 1024
        private const val JPEG_QUALITY = 85
    }
}
