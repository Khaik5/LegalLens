package com.example.lagallens.data.datasource.local

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import java.io.File

/**
 * Re-encodes a photo the backend does not accept (HEIC, HEIF, WebP) as JPEG.
 * The longest side is capped so very large photos stay readable for OCR without exceeding the upload limit.
 */
internal fun writeJpeg(resolver: ContentResolver, source: Uri, target: File) {
    // ImageDecoder applies the EXIF orientation of the photo.
    val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, source)) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val longestSide = maxOf(info.size.width, info.size.height)
        if (longestSide > MAX_EDGE) {
            val scale = MAX_EDGE.toFloat() / longestSide
            decoder.setTargetSize(
                (info.size.width * scale).toInt().coerceAtLeast(1),
                (info.size.height * scale).toInt().coerceAtLeast(1)
            )
        }
    }
    try {
        target.outputStream().buffered().use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) { "JPEG encoding failed" }
        }
    } finally {
        bitmap.recycle()
    }
}

private const val MAX_EDGE = 4096
private const val JPEG_QUALITY = 90
