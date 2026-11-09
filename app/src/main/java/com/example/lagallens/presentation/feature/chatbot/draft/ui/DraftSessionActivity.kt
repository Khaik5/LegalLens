package com.example.lagallens.presentation.feature.chatbot.draft.ui

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.lagallens.LegalLensApplication
import kotlinx.coroutines.launch

/** Keeps the Intent's original session across recreation and subsequent navigation. */
abstract class DraftSessionActivity : AppCompatActivity() {
    fun isDraftSessionValid(): Boolean = DraftSessionGuard.valid(application as LegalLensApplication, intent)
    protected fun guardDraftCreate(): Boolean {
        if (!isDraftSessionValid()) { finish(); return false }
        lifecycleScope.launch {
            (application as LegalLensApplication).observeSessionKeys().collect {
                if (!isDraftSessionValid()) finish()
            }
        }
        return true
    }
    /** Edge-to-edge, with system bar icons that stay readable on the light or dark theme background. */
    protected fun useEdgeToEdgeSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        val isNightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isNightMode
            isAppearanceLightNavigationBars = !isNightMode
        }
    }
    override fun onStart() { super.onStart(); if (!isDraftSessionValid()) finish() }
    override fun startActivity(intent: Intent?) { if (isDraftSessionValid()) super.startActivity(intent) }
    override fun startActivity(intent: Intent?, options: Bundle?) { if (isDraftSessionValid()) super.startActivity(intent, options) }
}
