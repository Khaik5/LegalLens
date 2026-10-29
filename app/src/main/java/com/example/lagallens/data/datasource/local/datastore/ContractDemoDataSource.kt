package com.example.lagallens.data.datasource.local.datastore

import android.content.Context
import com.example.lagallens.presentation.feature.contracts.contract.ContractListItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ContractDemoDataSource {
    private const val PREFERENCES_NAME = "contract_demo_data"
    private const val DELETED_IDS_KEY = "deleted_contract_ids"
    private const val RENAMED_TITLE_PREFIX = "renamed_title_"
    private val contracts = MutableStateFlow<List<ContractListItem>>(emptyList())
    val observableContracts = contracts.asStateFlow()
    private var initialized = false
    private lateinit var preferences: android.content.SharedPreferences

    @Synchronized
    fun initialize(context: Context) {
        if (!initialized) {
            preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            initialized = true
        }
        refresh()
    }

    @Synchronized
    fun getContract(id: String): ContractListItem? = if (initialized) {
        contracts.value.firstOrNull { it.id == id }
    } else {
        ContractListItem.allSamples.firstOrNull { it.id == id }
    }

    @Synchronized
    fun rename(id: String, title: String): Boolean {
        if (!initialized || contracts.value.none { it.id == id }) return false
        preferences.edit().putString(RENAMED_TITLE_PREFIX + id, title.trim()).apply()
        refresh()
        return true
    }

    @Synchronized
    fun delete(id: String): Boolean {
        if (!initialized || contracts.value.none { it.id == id }) return false
        val deletedIds = preferences.getStringSet(DELETED_IDS_KEY, emptySet()).orEmpty().toMutableSet()
        deletedIds += id
        preferences.edit().putStringSet(DELETED_IDS_KEY, deletedIds).apply()
        refresh()
        return true
    }

    @Synchronized
    private fun refresh() {
        if (!initialized) return
        val deletedIds = preferences.getStringSet(DELETED_IDS_KEY, emptySet()).orEmpty()
        contracts.value = ContractListItem.allSamples
            .filterNot { it.id in deletedIds }
            .map { item -> item.copy(titleOverride = preferences.getString(RENAMED_TITLE_PREFIX + item.id, null)) }
    }
}
