package com.example.lagallens.presentation.feature.chatbot.draft.save.pdf

import java.io.InputStream
import java.io.OutputStream

/** Stops subsequent writes when the owner session changes; already published bytes cannot be rolled back. */
internal fun copyWithSessionGuard(input: InputStream, output: OutputStream, guard: (() -> Unit) -> Unit) {
    val buffer = ByteArray(8192)
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        if (count == 0) continue
        guard { output.write(buffer, 0, count) }
    }
    guard { output.flush() }
}
