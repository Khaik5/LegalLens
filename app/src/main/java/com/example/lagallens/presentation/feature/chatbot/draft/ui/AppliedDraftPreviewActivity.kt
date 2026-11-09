package com.example.lagallens.presentation.feature.chatbot.draft.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityAppliedDraftPreviewBinding
import com.example.lagallens.databinding.ItemChatbotDraftSectionBinding
import com.example.lagallens.presentation.common.utils.applySystemBarInsets
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftUiState
import com.example.lagallens.domain.model.DraftStage
import com.example.lagallens.presentation.feature.chatbot.draft.save.SaveDraftCopyBottomSheet
import com.example.lagallens.domain.repository.latestDraft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppliedDraftPreviewActivity : DraftSessionActivity() {
    private val app by lazy { application as LegalLensApplication }
    private val ownerSession by lazy { app.currentSessionKey() }
    private fun isOwnerSession() = ownerSession == app.currentSessionKey()
    private lateinit var binding: ActivityAppliedDraftPreviewBinding
    private val hasD3 by lazy { intent.getBooleanExtra(EXTRA_HAS_D3, false) }
    private var draftContent: String = ""
    private var draftId: String? = null
    private var contractId: String? = null
    private var pendingSaveRequest = false
    private var activeVersion = ChatbotDraftUiState.APPROVED_AI_VERSION

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!guardDraftCreate()) return
        if (!DraftSessionGuard.valid(app, intent)) { finish(); return }
        if (ownerSession == null) { finish(); return }
        lifecycleScope.launch { app.observeSessionKeys().collect { if (!isOwnerSession()) finish() } }
        useEdgeToEdgeSystemBars()
        binding = ActivityAppliedDraftPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        val saved = intent.getBooleanExtra(EXTRA_SAVED, false)
        val version = intent.getStringExtra(EXTRA_VERSION) ?: ChatbotDraftUiState.APPROVED_AI_VERSION
        activeVersion = version
        contractId = intent.getStringExtra(EXTRA_CONTRACT_ID)
        if (saved) {
            binding.draftAppliedTitle.text = getString(R.string.chatbot_draft_saved_version_title, version)
            binding.draftAppliedSourceLine.setText(R.string.chatbot_draft_applied_source_line_saved)
            binding.draftAppliedVersionBadge.text = getString(R.string.chatbot_draft_saved_version_badge, version)
            binding.draftAppliedSave.isVisible = false
        }
        binding.draftAppliedBack.setOnClickListener { finish() }
        binding.draftAppliedSave.setOnClickListener {
            if (draftContent.isBlank()) {
                pendingSaveRequest = true
            } else {
                showSaveSheet()
            }
        }
        binding.draftAppliedManage.setOnClickListener { startActivity(ChatbotDraftVersionsActivity.createIntent(this, hasD3, contractId = contractId)) }
        binding.draftAppliedDocumentName.text = getString(R.string.chatbot_draft_document_name, version)
        loadContent(saved, version)
    }

    override fun onStart() { super.onStart(); if (!DraftSessionGuard.valid(app, intent)) finish() }

    private fun loadContent(saved: Boolean, version: String) {
        val repository = try { app.draftRepository.forSession(ownerSession) } catch (_: Exception) { showDraftUnavailable(); return }
        lifecycleScope.launch {
            val loaded = try {
                withContext(Dispatchers.IO) {
                    intent.getStringExtra(EXTRA_DRAFT_ID)?.let { repository.getDraft(it) } ?: repository.latestDraft(DraftStage.AI_PROPOSAL, contractId)
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Exception) {
                null
            }
            if (!isOwnerSession()) return@launch
            draftContent = loaded?.content.orEmpty()
            draftId = loaded?.draftId
            contractId = loaded?.contractId ?: contractId
            loaded?.title?.let { binding.draftAppliedDocumentName.text = it }
            if (draftContent.isBlank()) {
                showDraftUnavailable()
                return@launch
            }
            val section = ItemChatbotDraftSectionBinding.inflate(layoutInflater, binding.draftAppliedContractSections, false)
            section.draftSectionTitle.setText(R.string.chatbot_draft_saved_content_title)
            section.draftSectionBody.text = draftContent
            binding.draftAppliedContractSections.addView(section.root)
            if (pendingSaveRequest) {
                pendingSaveRequest = false
                showSaveSheet()
            }
        }
    }

    private fun showSaveSheet() {
        if (!isOwnerSession()) return
        if (draftContent.isBlank() || isFinishing || supportFragmentManager.findFragmentByTag(SaveDraftCopyBottomSheet.TAG) != null) return
        SaveDraftCopyBottomSheet.newInstance(activeVersion, draftContent, contractId, draftId)
            .show(supportFragmentManager, SaveDraftCopyBottomSheet.TAG)
    }

    private fun showDraftUnavailable() {
        android.widget.Toast.makeText(this, R.string.chatbot_draft_save_failed, android.widget.Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val EXTRA_HAS_D3 = "chatbot_applied_preview_has_d3"
        private const val EXTRA_SAVED = "chatbot_applied_preview_saved"
        private const val EXTRA_VERSION = "chatbot_applied_preview_version"
        private const val EXTRA_DRAFT_ID = "chatbot_applied_preview_draft_id"
        private const val EXTRA_CONTRACT_ID = "chatbot_applied_preview_contract_id"
        fun createIntent(context: Context, hasD3: Boolean = false, isSaved: Boolean = false, version: String = ChatbotDraftUiState.APPROVED_AI_VERSION, draftId: String? = null, contractId: String? = null): Intent =
            Intent(context, AppliedDraftPreviewActivity::class.java)
                .putExtra(EXTRA_HAS_D3, hasD3)
                .putExtra(EXTRA_SAVED, isSaved)
                .putExtra(EXTRA_VERSION, version)
                .putExtra(EXTRA_DRAFT_ID, draftId)
                .putExtra(EXTRA_CONTRACT_ID, contractId)
                .also { DraftSessionGuard.stamp(context, it) }
    }
}
