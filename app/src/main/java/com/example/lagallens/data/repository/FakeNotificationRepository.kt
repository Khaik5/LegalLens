package com.example.lagallens.data.repository

import android.content.Context
import com.example.lagallens.data.datasource.local.datastore.AccountStorageName
import com.example.lagallens.data.datasource.local.datastore.AuthLocalDataSource
import com.example.lagallens.domain.model.NotificationKind
import com.example.lagallens.domain.model.NotificationRecord
import com.example.lagallens.domain.repository.NotificationRepository

internal interface NotificationReadStore {
    fun getReadIds(): Set<String>
    fun updateReadIds(transform: (Set<String>) -> Set<String>)
}

/** Persistent, local-only notification fixtures for DEMO mode. */
class FakeNotificationRepository internal constructor(private val store: NotificationReadStore) : NotificationRepository {
    constructor(context: Context, sessionUserId: (() -> String?)? = null) : this(
        object : NotificationReadStore {
            private val appContext = context.applicationContext
            private fun preferences(): android.content.SharedPreferences {
                val userId = if (sessionUserId != null) sessionUserId.invoke() else AuthLocalDataSource(appContext).currentSessionUserId()
                return appContext.getSharedPreferences(AccountStorageName.forUser(PREFERENCES_NAME, userId), Context.MODE_PRIVATE)
            }
            override fun getReadIds(): Set<String> = preferences().getStringSet(KEY_READ_IDS, emptySet()).orEmpty().toSet()
            override fun updateReadIds(transform: (Set<String>) -> Set<String>) {
                val prefs = preferences()
                val ids = transform(prefs.getStringSet(KEY_READ_IDS, emptySet()).orEmpty().toSet())
                check(prefs.edit().putStringSet(KEY_READ_IDS, ids).commit())
            }
        }
    )

    override suspend fun getAll(): List<NotificationRecord> {
        val readIds = store.getReadIds()
        return DEMO_NOTIFICATIONS.map { it.copy(isUnread = it.id !in readIds && it.initiallyUnread) }
    }

    override suspend fun markRead(notificationId: String) {
        store.updateReadIds { it + notificationId }
    }

    override suspend fun markAllRead() {
        store.updateReadIds { DEMO_NOTIFICATIONS.mapTo(mutableSetOf()) { item -> item.id } }
    }

    private data class DemoNotification(
        val id: String,
        val contractId: String?,
        val kind: NotificationKind,
        val initiallyUnread: Boolean
    ) {
        fun copy(isUnread: Boolean) = NotificationRecord(id, contractId, kind, isUnread)
    }

    private companion object {
        const val PREFERENCES_NAME = "notification_demo_state"
        const val KEY_READ_IDS = "read_notification_ids"
        val DEMO_NOTIFICATIONS = listOf(
            DemoNotification("analysis-bitexco", "collaborator", NotificationKind.ANALYSIS, true),
            DemoNotification("ocr-collaborator", "collaborator", NotificationKind.OCR, true),
            DemoNotification("reminder-bitexco", null, NotificationKind.REMINDER, false),
            DemoNotification("document-ready", "collaborator", NotificationKind.ANALYSIS, false)
        )
    }
}
