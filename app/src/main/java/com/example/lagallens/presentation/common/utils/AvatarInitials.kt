package com.example.lagallens.presentation.common.utils

import java.text.Normalizer
import java.util.Locale

private val combiningMarks = Regex("""\p{M}+""")
private val whitespace = Regex("""\s+""")

/**
 * Avatar letters: the first letter of the first and of the last word, without accents and upper-cased, the same
 * rule the admin web uses. A single word gives one letter.
 */
fun String.avatarInitials(): String {
    val words = trim().split(whitespace).filter(String::isNotBlank)
    val picked = if (words.size > 1) listOf(words.first(), words.last()) else words
    return picked.joinToString("") { word ->
        Normalizer.normalize(word.take(1), Normalizer.Form.NFD)
            .replace(combiningMarks, "").replace('đ', 'd').replace('Đ', 'D')
    }.uppercase(Locale.ROOT)
}
