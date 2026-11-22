package com.example.lagallens.data.repository

import android.content.Context

/**
 * The idempotency key of each draft version's save-as-contract request. It outlives the process, so a request that
 * is repeated after the app was killed reuses the key and the backend returns the contract it already made.
 */
interface DraftContractSaveKeys {
    fun get(userId: String, draftId: Long, version: Int): String?
    fun put(userId: String, draftId: Long, version: Int, key: String)
}

class SharedPreferencesDraftContractSaveKeys(context: Context) : DraftContractSaveKeys {
    private val preferences = context.applicationContext.getSharedPreferences("remote_draft_contract_save_keys", Context.MODE_PRIVATE)

    override fun get(userId: String, draftId: Long, version: Int): String? = preferences.getString(entry(userId, draftId, version), null)

    // Committed before the request is sent, so a process death during the request cannot lose the key.
    override fun put(userId: String, draftId: Long, version: Int, key: String) {
        preferences.edit().putString(entry(userId, draftId, version), key).commit()
    }

    private fun entry(userId: String, draftId: Long, version: Int) = "$userId:$draftId:$version"
}

/** Keys that last as long as the process; the default for a repository that is not given a store. */
class InMemoryDraftContractSaveKeys : DraftContractSaveKeys {
    private val keys = java.util.concurrent.ConcurrentHashMap<String, String>()

    override fun get(userId: String, draftId: Long, version: Int): String? = keys["$userId:$draftId:$version"]

    override fun put(userId: String, draftId: Long, version: Int, key: String) {
        keys["$userId:$draftId:$version"] = key
    }
}
