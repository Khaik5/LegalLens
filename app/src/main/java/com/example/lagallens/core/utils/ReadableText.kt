package com.example.lagallens.core.utils

private val unbreakableSpaces = Regex("[\u00A0\u2007\u202F]")

/**
 * Text read from a PDF often uses no-break spaces between every word, so a text view cannot wrap at a space and
 * cuts the line in the middle of a word. For display they are plain spaces; the stored text is not touched.
 */
fun String.toReadableText(): String = replace(unbreakableSpaces, " ")
