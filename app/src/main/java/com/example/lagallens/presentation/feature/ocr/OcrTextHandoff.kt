package com.example.lagallens.presentation.feature.ocr

import java.util.UUID

/**
 * Carries long OCR text between the OCR screens. Intent extras share a Binder transaction buffer of about 1 MB
 * with the whole process, so recognised text of a multi-page contract can throw TransactionTooLargeException
 * when it travels as an extra. Only a short token is sent through the Intent; the text stays in memory here.
 *
 * Whoever receives text releases it: [peek] while the screen is alive (so it survives rotation) followed by
 * [release] once the screen is finishing, or [take] for a result that is read once. The text is never written to
 * disk. After process death it is gone, and the receiving screen must close instead of continuing with empty text.
 */
internal object OcrTextHandoff {
    private const val MAX_ENTRIES = 8
    private val entries = LinkedHashMap<String, String>()

    @Synchronized
    fun put(text: String): String {
        val token = UUID.randomUUID().toString()
        entries[token] = text
        // A screen that never received its text must not keep contract text in memory for the whole process life.
        while (entries.size > MAX_ENTRIES) entries.remove(entries.keys.first())
        return token
    }

    @Synchronized
    fun peek(token: String?): String? = token?.let(entries::get)

    @Synchronized
    fun take(token: String?): String? = token?.let(entries::remove)

    @Synchronized
    fun release(token: String?) {
        if (token != null) entries.remove(token)
    }
}
