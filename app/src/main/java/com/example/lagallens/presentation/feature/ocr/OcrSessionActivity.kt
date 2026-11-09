package com.example.lagallens.presentation.feature.ocr

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.lagallens.LegalLensApplication
import kotlinx.coroutines.launch

internal object OcrSession {
    const val EXTRA_OWNER = "ocr_owner_session"
    fun stamp(context: Context, target: Intent): Intent {
        val parent = (context as? Activity)?.intent
        val key = if (parent?.hasExtra(EXTRA_OWNER) == true) parent.getStringExtra(EXTRA_OWNER)
            else (context.applicationContext as LegalLensApplication).currentSessionKey()
        return target.putExtra(EXTRA_OWNER, key)
    }
}

abstract class OcrSessionActivity : AppCompatActivity() {
    protected val ownerSession: String get() = intent.getStringExtra(OcrSession.EXTRA_OWNER).orEmpty()
    protected fun isOcrSessionValid(): Boolean = ownerSession.isNotBlank() &&
        (application as LegalLensApplication).currentSessionKey() == ownerSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.alpha = 0f
        if (!isOcrSessionValid()) { finish(); return }
        lifecycleScope.launch {
            (application as LegalLensApplication).observeSessionKeys().collect {
                if (!isOcrSessionValid()) { window.decorView.alpha = 0f; finish() }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        if (!isOcrSessionValid()) { window.decorView.alpha = 0f; finish() }
        else window.decorView.alpha = 1f
    }
    override fun onPause() { window.decorView.alpha = 0f; super.onPause() }
}
