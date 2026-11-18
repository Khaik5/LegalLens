package com.example.lagallens.core.utils

// Mirrors android.util.Patterns.EMAIL_ADDRESS, also accepting apostrophes in names.
// Pure Kotlin so every authentication screen uses the same validation on JVM.
private val EMAIL_PATTERN = Regex(
    "[A-Za-z0-9+._%'-]{1,256}@" +
        "[A-Za-z0-9][A-Za-z0-9-]{0,64}" +
        "(?:\\.[A-Za-z0-9][A-Za-z0-9-]{0,25})+"
)

fun isValidEmail(value: String): Boolean = EMAIL_PATTERN.matches(value)
