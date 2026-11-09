package com.example.lagallens.data.repository

import android.content.Context
import androidx.annotation.StringRes
import com.example.lagallens.R
import com.example.lagallens.data.datasource.local.datastore.AccountStorageName
import com.example.lagallens.data.datasource.local.datastore.AuthLocalDataSource
import com.example.lagallens.domain.model.NotificationKind
import com.example.lagallens.domain.model.NotificationRecord
import com.example.lagallens.domain.repository.NotificationRepository

internal interface NotificationReadStore {
    fun getReadIds(): Set<String>
    fun text(@StringRes resId: Int): String = ""
    fun updateReadIds(transform: (Set<String>) -> Set<String>)
    fun <T> inSession(sessionKey: String?, action: () -> T): T = action()
}

/** Persistent, local-only notification fixtures for DEMO mode. */
class FakeNotificationRepository internal constructor(private val store: NotificationReadStore) : NotificationRepository {
    constructor(context: Context, sessionUserId: (() -> String?)? = null) : this(
        object : NotificationReadStore {
            private val appContext = context.applicationContext
            override fun text(resId: Int): String = appContext.getString(resId)
            override fun <T> inSession(sessionKey: String?, action: () -> T): T {
                if (sessionUserId != null) {
                    check(sessionUserId.invoke() == sessionKey) { "Session changed" }
                    return action()
                }
                val auth = AuthLocalDataSource(appContext)
                val scope = checkNotNull(auth.currentSessionScope()) { "No active DEMO session" }
                return auth.withSession(scope) {
                    check("${scope.userId}:${scope.generation}" == sessionKey) { "Session changed" }
                    action()
                }
            }
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

    private fun getAllLocal(): List<NotificationRecord> {
        val readIds = store.getReadIds()
        val now = System.currentTimeMillis()
        return DEMO_NOTIFICATIONS.map { item ->
            NotificationRecord(
                item.id, item.contractId, item.kind, item.id !in readIds && item.initiallyUnread,
                store.text(item.titleRes), store.text(item.descriptionRes), now - item.ageMillis
            )
        }
    }

    override suspend fun getAll(): List<NotificationRecord> = getAllLocal()
    override suspend fun getAllForSession(sessionKey: String?): List<NotificationRecord> = store.inSession(sessionKey) { getAllLocal() }
    override suspend fun markReadForSession(sessionKey: String?, notificationId: String) = store.inSession(sessionKey) {
        store.updateReadIds { it + notificationId }
    }
    override suspend fun markAllReadForSession(sessionKey: String?) = store.inSession(sessionKey) {
        store.updateReadIds { DEMO_NOTIFICATIONS.mapTo(mutableSetOf()) { item -> item.id } }
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
        val initiallyUnread: Boolean,
        @StringRes val titleRes: Int,
        @StringRes val descriptionRes: Int,
        val ageMillis: Long
    )

    private companion object {
        const val PREFERENCES_NAME = "notification_demo_state"
        const val KEY_READ_IDS = "read_notification_ids"
        const val MINUTE = 60_000L
        val DEMO_NOTIFICATIONS = listOf(
            DemoNotification("analysis-bitexco", "collaborator", NotificationKind.ANALYSIS, true, R.string.notification_analysis_complete_title, R.string.notification_analysis_complete_description, 10 * MINUTE),
            DemoNotification("ocr-collaborator", "collaborator", NotificationKind.OCR, true, R.string.notification_ocr_review_title, R.string.notification_ocr_review_description, 60 * MINUTE),
            DemoNotification("reminder-bitexco", null, NotificationKind.REMINDER, false, R.string.notification_expiring_title, R.string.notification_expiring_description, 180 * MINUTE),
            DemoNotification("document-ready", "collaborator", NotificationKind.ANALYSIS, false, R.string.notification_document_ready_title, R.string.notification_document_ready_description, 1440 * MINUTE)
        )
    }
}
