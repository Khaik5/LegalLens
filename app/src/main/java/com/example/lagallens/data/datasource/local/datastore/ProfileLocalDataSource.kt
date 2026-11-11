package com.example.lagallens.data.datasource.local.datastore

import android.content.Context
import com.example.lagallens.R
import com.example.lagallens.domain.model.UserProfile

data class ProfileEdits(val phone: String, val photoUri: String?)

/** Phone and avatar that older REAL builds kept on the device; read once to move them to the backend. */
internal interface ProfileLocalEdits {
    fun readEdits(userId: String): ProfileEdits
    fun clearEdits(userId: String)
}

class ProfileLocalDataSource(context: Context, private val sessionUserId: () -> String? = { null }, private val sessionEmail: () -> String? = { null }) : ProfileLocalEdits {
    private val appContext = context.applicationContext
    private fun preferences(): android.content.SharedPreferences? {
        val id = sessionUserId() ?: return null
        val canonical = runCatching { java.util.UUID.fromString(id).toString() }.getOrNull() ?: return null
        return appContext.getSharedPreferences("${PREFERENCES_NAME}_$canonical", Context.MODE_PRIVATE)
    }
    private val defaultName = context.getString(R.string.profile_demo_default_name)

    fun read(): UserProfile {
        val store = preferences()
        return UserProfile(
            name = store?.getString(KEY_NAME, null)?.takeIf(String::isNotBlank) ?: defaultName,
            email = sessionEmail().orEmpty(),
            phone = store?.getString(KEY_PHONE, "").orEmpty(),
            photoUri = store?.getString(KEY_PHOTO_URI, null)
        )
    }

    fun save(name: String, phone: String, photoUri: String?): UserProfile {
        val preferences = checkNotNull(preferences()) { "No active DEMO account" }
        check(preferences.edit()
            .putString(KEY_NAME, name.trim())
            .putString(KEY_PHONE, phone.trim())
            .putString(KEY_PHOTO_URI, photoUri)
            .commit())
        return read()
    }

    fun clearSessionData(): Boolean = true

    override fun readEdits(userId: String): ProfileEdits {
        val store = remotePreferences(userId)
        return ProfileEdits(store.getString(KEY_PHONE, "").orEmpty(), store.getString(KEY_PHOTO_URI, null))
    }

    override fun clearEdits(userId: String) {
        check(remotePreferences(userId).edit().clear().commit())
    }

    private fun remotePreferences(userId: String): android.content.SharedPreferences {
        val id = requireNotNull(userId.toLongOrNull()?.takeIf { it > 0 })
        return appContext.getSharedPreferences("profile_real_data_$id", Context.MODE_PRIVATE)
    }

    private companion object {
        const val PREFERENCES_NAME = "profile_demo_data"
        const val KEY_NAME = "name"
        const val KEY_PHONE = "phone"
        const val KEY_PHOTO_URI = "photo_uri"
    }
}
