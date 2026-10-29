package com.example.lagallens.presentation.feature.chatbot.draft.save.detail

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.lagallens.databinding.ActivitySavedDraftCopyDetailBinding
import com.example.lagallens.presentation.common.utils.applySystemBarInsets
import com.example.lagallens.presentation.feature.chatbot.draft.save.SavedDraftCopyStore
import com.example.lagallens.presentation.feature.chatbot.draft.save.pdf.DraftPdfExportBottomSheet
import com.example.lagallens.presentation.feature.chatbot.draft.ui.AppliedDraftPreviewActivity
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftUiState
import com.example.lagallens.presentation.feature.chatbot.draft.ui.ChatbotDraftPreviewActivity

class SavedDraftCopyDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySavedDraftCopyDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        binding = ActivitySavedDraftCopyDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        binding.savedDraftBack.setOnClickListener { finish() }
        val version = intent.getStringExtra(EXTRA_VERSION) ?: ChatbotDraftUiState.APPROVED_AI_VERSION
        binding.savedDraftTitle.text = SavedDraftCopyStore.name(this, version).orEmpty()
        binding.savedDraftView.setOnClickListener {
            if (version == ChatbotDraftUiState.APPROVED_AI_VERSION) {
                startActivity(AppliedDraftPreviewActivity.createIntent(this, isSaved = true, version = version))
            } else {
                startActivity(ChatbotDraftPreviewActivity.createIntent(this, version, savedContent = SavedDraftCopyStore.content(this, version)))
            }
        }
        binding.savedDraftExport.setOnClickListener { showExportSheet() }
        if (intent.getBooleanExtra(EXTRA_OPEN_EXPORT, false)) binding.root.post { showExportSheet() }
    }

    private fun showExportSheet() {
        if (supportFragmentManager.findFragmentByTag(DraftPdfExportBottomSheet.TAG) == null) {
            DraftPdfExportBottomSheet.newInstance(intent.getStringExtra(EXTRA_VERSION) ?: ChatbotDraftUiState.APPROVED_AI_VERSION).show(supportFragmentManager, DraftPdfExportBottomSheet.TAG)
        }
    }

    companion object {
        private const val EXTRA_OPEN_EXPORT = "saved_draft_detail_open_export"
        private const val EXTRA_VERSION = "saved_draft_detail_version"
        fun createIntent(context: Context, version: String = ChatbotDraftUiState.APPROVED_AI_VERSION, openExport: Boolean = false) =
            Intent(context, SavedDraftCopyDetailActivity::class.java)
                .putExtra(EXTRA_OPEN_EXPORT, openExport)
                .putExtra(EXTRA_VERSION, version)
    }
}
