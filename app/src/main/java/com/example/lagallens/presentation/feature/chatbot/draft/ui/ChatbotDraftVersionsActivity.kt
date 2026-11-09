package com.example.lagallens.presentation.feature.chatbot.draft.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.BuildConfig
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityChatbotDraftVersionsBinding
import com.example.lagallens.presentation.common.utils.applySystemBarInsets
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftUiState
import com.example.lagallens.domain.model.Draft
import com.example.lagallens.domain.model.DraftStage
import com.example.lagallens.domain.repository.latestDraft
import com.example.lagallens.presentation.feature.documentviewer.ui.DocumentViewerActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatbotDraftVersionsActivity : DraftSessionActivity() {
    private val app by lazy { application as LegalLensApplication }
    private val ownerSession by lazy { app.currentSessionKey() }
    private fun isOwnerSession() = ownerSession == app.currentSessionKey()
    private lateinit var binding: ActivityChatbotDraftVersionsBinding
    private val hasD3 by lazy { intent.getBooleanExtra(EXTRA_HAS_D3, false) }
    private val contractId by lazy { intent.getStringExtra(EXTRA_CONTRACT_ID) }
    private var latestByStage: Map<DraftStage, Draft> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!guardDraftCreate()) return
        if (!DraftSessionGuard.valid(app, intent)) { finish(); return }
        if (ownerSession == null) { finish(); return }
        val repository = try { app.draftRepository.forSession(ownerSession) } catch (_: Exception) { finish(); return }
        lifecycleScope.launch { app.observeSessionKeys().collect { if (!isOwnerSession()) finish() } }
        useEdgeToEdgeSystemBars()
        binding = ActivityChatbotDraftVersionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        binding.draftVersionsBack.setOnClickListener { finish() }
        binding.draftVersionD2View.setOnClickListener { startActivity(AppliedDraftPreviewActivity.createIntent(this, hasD3, draftId = latestByStage[DraftStage.AI_PROPOSAL]?.draftId, contractId = contractId)) }
        binding.draftVersionD1View.setOnClickListener { startActivity(ChatbotDraftPreviewActivity.createIntent(this, ChatbotDraftUiState.DRAFT_VERSION, draftId = latestByStage[DraftStage.ORIGINAL]?.draftId, contractId = contractId)) }
        binding.draftVersionD1Copy.setOnClickListener { confirmCopyD1() }
        binding.draftVersionOriginalView.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val result = withContext(Dispatchers.IO) {
                        val draft = repository.latestDraft(DraftStage.ORIGINAL, contractId)
                        check(isOwnerSession())
                        val contract = draft?.contractId?.let { app.contractRepository.getContract(it) }
                        val document = contract?.let { app.documentRepository.getDocument(it.documentId) }
                        contract to (contract?.fileUri ?: document?.uri)
                    }
                    if (!isOwnerSession()) return@launch
                    val contract = result.first
                    val uri = result.second
                    if (uri.isNullOrBlank()) {
                        android.widget.Toast.makeText(this@ChatbotDraftVersionsActivity, R.string.contract_detail_feature_unavailable, android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        startActivity(DocumentViewerActivity.createIntent(this@ChatbotDraftVersionsActivity, Uri.parse(uri), contract?.title.orEmpty()))
                    }
                } catch (cancellationException: CancellationException) {
                    throw cancellationException
                } catch (_: Exception) {
                    if (isOwnerSession()) android.widget.Toast.makeText(this@ChatbotDraftVersionsActivity, R.string.contract_detail_feature_unavailable, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.draftVersionsCompare.setOnClickListener { startActivity(CompareDraftVersionsActivity.createIntent(this, contractId)) }
        binding.draftVersionD3View.setOnClickListener { startActivity(ChatbotDraftPreviewActivity.createIntent(this, ChatbotDraftUiState.COPY_VERSION, draftId = latestByStage[DraftStage.COPY]?.draftId, contractId = contractId)) }
    }

    override fun onStart() {
        super.onStart()
        if (!DraftSessionGuard.valid(app, intent)) { finish(); return }
        loadVersions()
    }

    /** Cards and rows follow the stored versions of this contract, so they stay current after returning from a copy. */
    private fun loadVersions() {
        val repository = try { app.draftRepository.forSession(ownerSession) } catch (_: Exception) { return }
        lifecycleScope.launch {
            val drafts = try {
                withContext(Dispatchers.IO) { repository.getDrafts() }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Exception) {
                android.widget.Toast.makeText(this@ChatbotDraftVersionsActivity, R.string.chatbot_draft_load_failed, android.widget.Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (!isOwnerSession()) return@launch
            val owned = drafts.filter { contractId == null || it.contractId == contractId }
            latestByStage = owned.groupBy { it.stage }.mapValues { (_, versions) -> versions.maxBy { it.versionNo } }
            (binding.draftVersionD2View.parent as android.view.View).isVisible = DraftStage.AI_PROPOSAL in latestByStage
            (binding.draftVersionD1View.parent.parent as android.view.View).isVisible = DraftStage.ORIGINAL in latestByStage
            binding.draftVersionD3Card.isVisible = DraftStage.COPY in latestByStage
            binding.draftVersionsCompare.isEnabled = DraftStage.ORIGINAL in latestByStage && DraftStage.AI_PROPOSAL in latestByStage
            binding.draftVersionOthers.removeAllViews()
            listOf(DraftStage.REGENERATED, DraftStage.MANUAL_EDIT).mapNotNull(latestByStage::get).forEach { draft ->
                val row = layoutInflater.inflate(R.layout.item_chatbot_draft_version_row, binding.draftVersionOthers, false) as android.widget.TextView
                row.text = getString(R.string.chatbot_draft_version_row, draft.stage.legacyKey, draft.title)
                row.setOnClickListener {
                    startActivity(ChatbotDraftPreviewActivity.createIntent(this@ChatbotDraftVersionsActivity, draft.stage.legacyKey, draftId = draft.draftId, contractId = contractId))
                }
                binding.draftVersionOthers.addView(row)
            }
        }
    }

    private fun confirmCopyD1() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.chatbot_draft_copy_confirm_title)
            .setMessage(R.string.chatbot_draft_copy_confirm_body)
            .setNegativeButton(R.string.chatbot_draft_ai_cancel, null)
            .setPositiveButton(R.string.chatbot_draft_copy_confirm_action) { _, _ ->
                if (!isOwnerSession()) return@setPositiveButton
                startActivity(CopyDraftVersionActivity.createIntent(this, BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_PREVIEW_COPY_ERROR, false), latestByStage[DraftStage.ORIGINAL]?.draftId, contractId))
            }
            .show()
    }

    companion object {
        private const val EXTRA_HAS_D3 = "chatbot_draft_versions_has_d3"
        private const val EXTRA_CONTRACT_ID = "chatbot_draft_versions_contract_id"
        const val EXTRA_PREVIEW_COPY_ERROR = "chatbot_draft_versions_copy_error"
        fun createIntent(context: Context, hasD3: Boolean = false, previewCopyError: Boolean = false, contractId: String? = null) =
            Intent(context, ChatbotDraftVersionsActivity::class.java)
                .putExtra(EXTRA_HAS_D3, hasD3)
                .putExtra(EXTRA_PREVIEW_COPY_ERROR, previewCopyError)
                .putExtra(EXTRA_CONTRACT_ID, contractId)
                .also { DraftSessionGuard.stamp(context, it) }
    }
}
