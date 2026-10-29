package com.example.lagallens.data.datasource.local.datastore

import java.security.MessageDigest

/** Preference lookup key only; the actual account identifier is a random UUID. */
internal object DemoAccountKeys {
    fun lookup(email: String): String {
        val normalized = email.trim().lowercase(java.util.Locale.ROOT)
        require(normalized.isNotBlank())
        return MessageDigest.getInstance("SHA-256")
            .digest(normalized.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}
