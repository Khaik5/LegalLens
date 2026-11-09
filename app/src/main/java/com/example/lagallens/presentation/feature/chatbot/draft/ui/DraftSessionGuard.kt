package com.example.lagallens.presentation.feature.chatbot.draft.ui

import android.content.Context
import android.content.Intent
import android.app.Activity
import com.example.lagallens.LegalLensApplication

internal object DraftSessionGuard {
    private const val EXTRA_SESSION = "draft_owner_session"
    fun stamp(context: Context, intent: Intent) {
        val app = context.applicationContext as LegalLensApplication
        val parent = (context as? Activity)?.intent
        val parentKey = if (parent?.hasExtra(EXTRA_SESSION) == true) EXTRA_SESSION else "chatbot_session_key"
        val owner = propagatedOwner(parent?.hasExtra(parentKey) == true, parent?.getStringExtra(parentKey), app.currentSessionKey())
        intent.putExtra(EXTRA_SESSION, owner)
    }
    fun valid(app: LegalLensApplication, intent: Intent): Boolean =
        matches(intent.getStringExtra(EXTRA_SESSION), app.currentSessionKey())
    fun isBound(intent: Intent): Boolean = intent.hasExtra(EXTRA_SESSION)
    fun owner(intent: Intent): String? = intent.getStringExtra(EXTRA_SESSION)
    fun stampIfParentBound(context: Context, intent: Intent) {
        if ((context as? Activity)?.intent?.hasExtra(EXTRA_SESSION) == true) stamp(context, intent)
    }
    fun matches(expected: String?, current: String?): Boolean = expected != null && expected == current
    fun propagatedOwner(parentHasOwner: Boolean, parentOwner: String?, current: String?): String? = if (parentHasOwner) parentOwner else current
}
