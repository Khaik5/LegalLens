package com.example.lagallens.presentation.feature.chatbot.draft.save

import android.content.Context
import com.example.lagallens.data.datasource.local.datastore.AccountStorageName
import com.example.lagallens.data.datasource.local.datastore.AuthLocalDataSource

object SavedDraftCopyStore {
    private const val PREFERENCES_NAME = "saved_draft_copies"
    private const val DEFAULT_DRAFT_NAME = "Hợp đồng lao động DEMO"

    fun save(context: Context, version: String, name: String): Boolean {
        return save(context, version, name, "")
    }

    fun save(context: Context, version: String, name: String, content: String): Boolean {
        val title = name.trim().ifBlank { "$DEFAULT_DRAFT_NAME — $version" }
        return preferences(context).edit()
            .putString(nameKey(version), title)
            .putString(contentKey(version), content)
            .putString(updatedKey(version), System.currentTimeMillis().toString())
            .commit()
    }

    fun name(context: Context, version: String): String? = preferences(context).getString(nameKey(version), null)

    fun isSaved(context: Context, version: String): Boolean = name(context, version) != null

    fun content(context: Context, version: String): String? = preferences(context).getString(contentKey(version), null)

    fun updatedAt(context: Context, version: String): Long? = preferences(context).getString(updatedKey(version), null)?.toLongOrNull()

    fun delete(context: Context, version: String): Boolean = preferences(context).edit()
        .remove(nameKey(version)).remove(contentKey(version)).remove(updatedKey(version)).commit()

    private fun preferences(context: Context): android.content.SharedPreferences {
        val app = context.applicationContext
        val scope = checkNotNull(AuthLocalDataSource(app).currentSessionScope()) { "No active DEMO session" }
        return app.getSharedPreferences(AccountStorageName.forUser(PREFERENCES_NAME, scope.userId), Context.MODE_PRIVATE)
    }

    private fun nameKey(version: String) = "draft_${version.lowercase().replace(Regex("[^a-z0-9]+"), "_")}"
    private fun contentKey(version: String) = "${nameKey(version)}_content"
    private fun updatedKey(version: String) = "${nameKey(version)}_updated_at"
}
