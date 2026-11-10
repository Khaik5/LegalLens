package com.example.lagallens.data.datasource.remote

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.io.File

internal class UploadProgressRequestBody(
    private val file: File,
    private val mediaType: MediaType,
    private val onProgress: (Int) -> Unit
) : RequestBody() {
    private val length = file.length()

    override fun contentType(): MediaType = mediaType
    override fun contentLength(): Long = length

    override fun writeTo(sink: BufferedSink) {
        file.source().use { source ->
            var sent = 0L
            var lastPercent = 0
            while (sent < length) {
                val count = source.read(sink.buffer, minOf(8192L, length - sent))
                if (count < 0) throw java.io.EOFException("Upload source truncated")
                sink.emitCompleteSegments()
                sent += count
                val percent = (sent * 100 / length).toInt().coerceAtMost(99)
                if (percent > lastPercent) {
                    onProgress(percent)
                    lastPercent = percent
                }
            }
        }
    }
}
