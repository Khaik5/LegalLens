package com.example.lagallens.presentation.feature.chatbot.draft.save.detail

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.di.AppDataMode
import com.example.lagallens.di.DataModeConfig
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
    // The draft this screen shows; the export must use it, because the version label ("D2") alone cannot
    // identify one backend version and would fall back to the newest draft of that stage.
    private var draftId: String? = null
    private val viewModel by viewModels<SavedDraftCopyDetailViewModel> {
        val app = application as LegalLensApplication
        val owner = DraftSessionGuard.owner(intent)
        SavedDraftCopyDetailViewModel.Factory(owner, app::currentSessionKey) { app.draftRepository.forSession(owner) }
    }

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
        draftId = intent.getStringExtra(EXTRA_DRAFT_ID)
        binding.savedDraftTitle.text = getString(R.string.draft_default_document_name, version)
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
        bindSaveAsContract()
        if (intent.getBooleanExtra(EXTRA_OPEN_EXPORT, false)) binding.root.post { showExportSheet() }
    }

    /** Only a real backend can make the contract; the offline demo has nowhere to save one. */
    private fun bindSaveAsContract() {
        binding.savedDraftSaveContract.isVisible = DataModeConfig.current == AppDataMode.REAL
        binding.savedDraftSaveContract.setOnClickListener {
            val id = draftId ?: return@setOnClickListener
            viewModel.onEvent(SavedDraftCopyDetailUiEvent.SaveAsContractClicked(id))
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { renderSaveAsContract(it) } }
                launch { viewModel.uiEffect.collect { effect -> if (effect is SavedDraftCopyDetailUiEffect.ShowMessage) Toast.makeText(this@SavedDraftCopyDetailActivity, effect.messageRes, Toast.LENGTH_LONG).show() } }
            }
        }
    }

    private fun renderSaveAsContract(state: SavedDraftCopyDetailUiState) = with(binding.savedDraftSaveContract) {
        isEnabled = state.saveAsContractStatus == SaveAsContractStatus.IDLE
        setText(
            when (state.saveAsContractStatus) {
                SaveAsContractStatus.IDLE -> R.string.draft_saved_contract_button
                SaveAsContractStatus.SAVING -> R.string.draft_saved_contract_saving
                SaveAsContractStatus.SAVED -> R.string.draft_saved_contract_done
            }
        )
    }

    private fun showExportSheet() {
        if (supportFragmentManager.findFragmentByTag(DraftPdfExportBottomSheet.TAG) == null) {
            DraftPdfExportBottomSheet.newInstance(
                draftId ?: intent.getStringExtra(EXTRA_VERSION) ?: ChatbotDraftUiState.APPROVED_AI_VERSION,
                binding.savedDraftTitle.text.toString()
            ).show(supportFragmentManager, DraftPdfExportBottomSheet.TAG)
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
