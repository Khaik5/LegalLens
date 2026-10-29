package com.example.lagallens.data.datasource.local.datastore

import java.util.UUID

/** Namespaces user data without reading or modifying ownerless legacy preferences. */
internal object AccountStorageName {
    fun forUser(base: String, userId: String?): String {
        val canonical = try { UUID.fromString(requireNotNull(userId)).toString() }
        catch (_: IllegalArgumentException) { throw IllegalStateException("No valid active account") }
        return "${base}_$canonical"
    }
}
