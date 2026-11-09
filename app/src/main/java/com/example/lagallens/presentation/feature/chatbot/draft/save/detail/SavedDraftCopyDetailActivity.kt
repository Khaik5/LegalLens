package com.example.lagallens.presentation.feature.chatbot.draft.save.detail

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import com.example.lagallens.presentation.feature.chatbot.draft.ui.DraftSessionActivity
import com.example.lagallens.presentation.feature.chatbot.draft.ui.DraftSessionGuard
import androidx.core.view.WindowCompat
import com.example.lagallens.databinding.ActivitySavedDraftCopyDetailBinding
import com.example.lagallens.presentation.common.utils.applySystemBarInsets
import androidx.lifecycle.lifecycleScope
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.R
import com.example.lagallens.domain.model.DraftStage
import com.example.lagallens.domain.repository.latestDraft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.example.lagallens.presentation.feature.chatbot.draft.save.pdf.DraftPdfExportBottomSheet
import com.example.lagallens.presentation.feature.chatbot.draft.ui.AppliedDraftPreviewActivity
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftUiState
import com.example.lagallens.presentation.feature.chatbot.draft.ui.ChatbotDraftPreviewActivity

class SavedDraftCopyDetailActivity : DraftSessionActivity() {
    private lateinit var binding: ActivitySavedDraftCopyDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!guardDraftCreate()) return
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        binding = ActivitySavedDraftCopyDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        binding.savedDraftBack.setOnClickListener { finish() }
        val version = intent.getStringExtra(EXTRA_VERSION) ?: ChatbotDraftUiState.APPROVED_AI_VERSION
        val contractId = intent.getStringExtra(EXTRA_CONTRACT_ID)
        var draftId = intent.getStringExtra(EXTRA_DRAFT_ID)
        binding.savedDraftTitle.text = getString(R.string.chatbot_draft_document_name, version)
        lifecycleScope.launch {
            val app = application as LegalLensApplication
            val draft = try {
                val repository = app.draftRepository.forSession(app.currentSessionKey())
                val stage = DraftStage.fromLegacyKey(version) ?: DraftStage.ORIGINAL
                draftId?.let { repository.getDraft(it) } ?: repository.latestDraft(stage, contractId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                null
            }
            draft?.let {
                draftId = it.draftId
                binding.savedDraftTitle.text = it.title
            }
        }
        binding.savedDraftView.setOnClickListener {
            if (version == ChatbotDraftUiState.APPROVED_AI_VERSION) {
                startActivity(AppliedDraftPreviewActivity.createIntent(this, isSaved = true, version = version, draftId = draftId, contractId = contractId))
            } else {
                startActivity(ChatbotDraftPreviewActivity.createIntent(this, version, draftId = draftId, contractId = contractId))
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
        private const val EXTRA_DRAFT_ID = "saved_draft_detail_draft_id"
        private const val EXTRA_CONTRACT_ID = "saved_draft_detail_contract_id"
        fun createIntent(context: Context, version: String = ChatbotDraftUiState.APPROVED_AI_VERSION, openExport: Boolean = false, draftId: String? = null, contractId: String? = null) =
            Intent(context, SavedDraftCopyDetailActivity::class.java)
                .putExtra(EXTRA_OPEN_EXPORT, openExport)
                .putExtra(EXTRA_VERSION, version)
                .putExtra(EXTRA_DRAFT_ID, draftId)
                .putExtra(EXTRA_CONTRACT_ID, contractId)
            .also { DraftSessionGuard.stamp(context, it) }
    }
}
