package com.example.lagallens.presentation.feature.chatbot.draft.ui

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import android.view.LayoutInflater
import android.widget.EditText
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.example.lagallens.BuildConfig
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityChatbotDraftPreviewBinding
import com.example.lagallens.databinding.DialogChatbotDraftRegenerationBinding
import com.example.lagallens.presentation.common.utils.applySystemBarInsets
import com.example.lagallens.presentation.feature.chatbot.draft.DraftContentFactory
import com.example.lagallens.presentation.feature.chatbot.draft.adapter.ChatbotDraftPageAdapter
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftPage
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftSection
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftUiEvent
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftUiState
import com.example.lagallens.domain.model.DraftStage
import com.example.lagallens.presentation.feature.chatbot.draft.viewmodel.ChatbotDraftViewModel
import com.example.lagallens.presentation.feature.chatbot.draft.save.SaveDraftCopyBottomSheet
import com.example.lagallens.presentation.feature.chatbot.draft.save.SavedDraftCopyStore
import com.example.lagallens.presentation.feature.chatbot.draft.save.detail.SavedDraftCopyDetailActivity
import kotlinx.coroutines.launch

class ChatbotDraftPreviewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatbotDraftPreviewBinding
    private val viewModel by viewModels<ChatbotDraftViewModel>()
    private var renderedContent: String? = null
    private val pageAdapter by lazy {
        val savedContent = intent.getStringExtra(EXTRA_SAVED_CONTENT)
        ChatbotDraftPageAdapter(if (savedContent != null) savedContentPages(savedContent) else draftPages(intent.getStringExtra(EXTRA_VERSION) ?: ChatbotDraftUiState.DRAFT_VERSION, intent.getStringExtra(EXTRA_MANUAL_BODY)))
    }
    private val aiEditResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_EDIT_REQUEST) {
            showAiEditSheet(
                result.data?.getStringExtra(ChatbotDraftAiEditingActivity.EXTRA_SCOPE)
                    ?: com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftAiEditUiState.SELECTED_CLAUSE_SCOPE,
                result.data?.getStringExtra(ChatbotDraftAiEditingActivity.EXTRA_REQUEST)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityChatbotDraftPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        binding.draftPagePager.adapter = pageAdapter
        binding.draftPagePager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.onEvent(ChatbotDraftUiEvent.PageSelected(position))
            }
        })
        bindActions()
        val version = intent.getStringExtra(EXTRA_VERSION) ?: ChatbotDraftUiState.DRAFT_VERSION
        viewModel.onEvent(
            ChatbotDraftUiEvent.Opened(
                version = version,
                initialTitle = getString(R.string.chatbot_draft_document_name, version),
                initialContent = intent.getStringExtra(EXTRA_SAVED_CONTENT) ?: draftContent(version),
                contractId = intent.getStringExtra(EXTRA_CONTRACT_ID),
                draftId = intent.getStringExtra(EXTRA_DRAFT_ID)
            )
        )
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun bindActions() = with(binding) {
        draftBack.setOnClickListener { finish() }
        draftMore.setOnClickListener { showUnavailableMessage(R.string.chatbot_draft_more_unavailable) }
        draftPrevious.setOnClickListener { viewModel.onEvent(ChatbotDraftUiEvent.PreviousClicked) }
        draftNext.setOnClickListener { viewModel.onEvent(ChatbotDraftUiEvent.NextClicked) }
        draftZoomOut.setOnClickListener { viewModel.onEvent(ChatbotDraftUiEvent.ZoomOutClicked) }
        draftZoomIn.setOnClickListener { viewModel.onEvent(ChatbotDraftUiEvent.ZoomInClicked) }
        draftEdit.setOnClickListener {
            val state = viewModel.uiState.value
            startActivity(ChatbotDraftManualEditActivity.createIntent(this@ChatbotDraftPreviewActivity, state.content ?: intent.getStringExtra(EXTRA_MANUAL_BODY), state.draftId, state.contractId))
        }
        draftRegenerate.setOnClickListener { showRegenerationConfirmation() }
        draftExport.setOnClickListener {
            val version = viewModel.uiState.value.version
            if (SavedDraftCopyStore.isSaved(this@ChatbotDraftPreviewActivity, version)) {
                startActivity(SavedDraftCopyDetailActivity.createIntent(this@ChatbotDraftPreviewActivity, version, openExport = true))
            } else {
                androidx.appcompat.app.AlertDialog.Builder(this@ChatbotDraftPreviewActivity)
                    .setTitle(R.string.draft_export_unsaved_title)
                    .setMessage(R.string.draft_export_unsaved_body)
                    .setPositiveButton(R.string.draft_export_unsaved_action) { _, _ ->
                        showSaveDraftCopy(version)
                    }
                    .setNegativeButton(R.string.chatbot_draft_ai_cancel, null)
                    .show()
            }
        }
        draftAskAi.setOnClickListener { showAiEditSheet() }
        draftSave.setOnClickListener { showSaveDraftCopy(viewModel.uiState.value.version) }
    }

    private fun showSaveDraftCopy(version: String) {
        if (supportFragmentManager.findFragmentByTag(SaveDraftCopyBottomSheet.TAG) == null) {
        val state = viewModel.uiState.value
        SaveDraftCopyBottomSheet.newInstance(version, state.content ?: draftContent(version), state.contractId, state.draftId)
            .show(supportFragmentManager, SaveDraftCopyBottomSheet.TAG)
        }
    }

    private fun render(state: ChatbotDraftUiState) = with(binding) {
        draftContractName.text = state.title ?: SavedDraftCopyStore.name(this@ChatbotDraftPreviewActivity, state.version)
            ?: getString(R.string.chatbot_draft_document_name, state.version)
        draftVersion.setText(
            if (state.version == ChatbotDraftUiState.REGENERATED_VERSION) {
                R.string.chatbot_draft_regenerated_version_metadata
            } else if (state.version == ChatbotDraftUiState.MANUAL_VERSION) {
                R.string.chatbot_draft_manual_version_metadata
            } else if (state.version == ChatbotDraftUiState.APPROVED_AI_VERSION) {
                R.string.chatbot_draft_ai_version_metadata
            } else if (state.version == ChatbotDraftUiState.COPY_VERSION) {
                R.string.chatbot_draft_copy_version_metadata
            } else {
                R.string.chatbot_draft_version_metadata
            }
        )
        draftPageCount.text = getString(R.string.chatbot_draft_page_count, state.currentPage + 1, state.pageCount)
        draftZoomLevel.text = getString(R.string.chatbot_draft_zoom_level, state.zoomPercent)
        draftPrevious.isEnabled = state.currentPage > 0
        draftNext.isEnabled = state.currentPage < state.pageCount - 1
        draftZoomOut.isEnabled = state.zoomPercent > MIN_ZOOM_PERCENT
        draftZoomIn.isEnabled = state.zoomPercent < MAX_ZOOM_PERCENT
        if (draftPagePager.currentItem != state.currentPage) draftPagePager.setCurrentItem(state.currentPage, true)
        pageAdapter.setZoomPercent(state.zoomPercent)
        state.content?.let { content ->
            if (renderedContent != content) {
                renderedContent = content
                pageAdapter.setPages(savedContentPages(content))
            }
        }
    }

    private fun showRegenerationConfirmation() {
        val dialog = Dialog(this)
        val dialogBinding = DialogChatbotDraftRegenerationBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.38f)
            setLayout(resources.getDimensionPixelSize(R.dimen.dp_48).let { resources.displayMetrics.widthPixels - it }, WindowManager.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.CENTER)
        }
        dialogBinding.draftRegenerationCancel.setOnClickListener { dialog.dismiss() }
        dialogBinding.draftRegenerationConfirm.setOnClickListener {
            dialog.dismiss()
            startActivity(
                ChatbotDraftRegenerationActivity.createIntent(
                    this,
                    viewModel.uiState.value.version,
                    BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_PREVIEW_REGENERATION_ERROR, false),
                    viewModel.uiState.value.draftId,
                    viewModel.uiState.value.contractId
                )
            )
        }
        dialog.show()
        dialog.window?.setLayout(
            resources.displayMetrics.widthPixels - 2 * resources.getDimensionPixelSize(R.dimen.dp_24),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun showUnavailableMessage(messageRes: Int) {
        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
    }

    private fun showAiEditSheet(scope: String = com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftAiEditUiState.SELECTED_CLAUSE_SCOPE, request: String? = null) {
        val sheet = BottomSheetDialog(this)
        val sheetBinding = com.example.lagallens.databinding.BottomSheetChatbotDraftAiEditBinding.inflate(layoutInflater)
        sheet.setContentView(sheetBinding.root)
        var selectedScope = scope
        fun renderScope() {
            val whole = selectedScope == com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftAiEditUiState.WHOLE_CONTRACT_SCOPE
            sheetBinding.draftAiScopeNote.setText(if (whole) R.string.chatbot_draft_ai_scope_note_whole else R.string.chatbot_draft_ai_scope_note_selected)
            sheetBinding.draftAiWholeScope.alpha = if (whole) 1f else 0.62f
            sheetBinding.draftAiSelectedScope.alpha = if (whole) 0.62f else 1f
        }
        sheetBinding.draftAiRequest.setText(request ?: getString(R.string.chatbot_draft_ai_default_request))
        sheetBinding.draftAiWholeScope.setOnClickListener {
            selectedScope = com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftAiEditUiState.WHOLE_CONTRACT_SCOPE
            renderScope()
        }
        sheetBinding.draftAiSelectedScope.setOnClickListener {
            selectedScope = com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftAiEditUiState.SELECTED_CLAUSE_SCOPE
            renderScope()
        }
        sheetBinding.draftAiSuggestionTerm.setOnClickListener { sheetBinding.draftAiRequest.setText(R.string.chatbot_draft_ai_default_request) }
        sheetBinding.draftAiSuggestionPayment.setOnClickListener { sheetBinding.draftAiRequest.setText(R.string.chatbot_draft_ai_suggestion_payment_request) }
        sheetBinding.draftAiCancel.setOnClickListener { sheet.dismiss() }
        sheetBinding.draftAiSubmit.setOnClickListener {
            val text = sheetBinding.draftAiRequest.text?.toString().orEmpty().trim()
            if (text.isNotEmpty()) {
                sheet.dismiss()
                aiEditResultLauncher.launch(
                    ChatbotDraftAiEditingActivity.createIntent(
                        this,
                        selectedScope,
                        text,
                        BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_PREVIEW_AI_ERROR, false),
                        BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_PREVIEW_APPLY_ERROR, false),
                        viewModel.uiState.value.draftId,
                        viewModel.uiState.value.contractId
                    )
                )
            } else sheetBinding.draftAiRequest.error = getString(R.string.chatbot_draft_ai_prompt_required)
        }
        renderScope()
        sheet.show()
        sheet.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    private fun draftPages(version: String, manualBody: String?) = listOf(
        ChatbotDraftPage(
            headingRes = R.string.chatbot_draft_contract_heading,
            introductionRes = R.string.chatbot_draft_contract_introduction,
            sections = listOf(
                ChatbotDraftSection(R.string.chatbot_draft_section_1_title, R.string.chatbot_draft_section_1_body),
                ChatbotDraftSection(R.string.chatbot_draft_section_2_title, R.string.chatbot_draft_section_2_body, manualBody.takeIf { version == ChatbotDraftUiState.MANUAL_VERSION })
            )
        ),
        ChatbotDraftPage(
            sections = listOf(
                ChatbotDraftSection(R.string.chatbot_draft_section_2_title, R.string.chatbot_draft_section_2_body, manualBody.takeIf { version == ChatbotDraftUiState.MANUAL_VERSION }),
                ChatbotDraftSection(R.string.chatbot_draft_section_3_title, R.string.chatbot_draft_section_3_body, getString(R.string.chatbot_draft_section_3_approved_body).takeIf { version == ChatbotDraftUiState.APPROVED_AI_VERSION })
            )
        ),
        ChatbotDraftPage(
            sections = listOf(
                ChatbotDraftSection(R.string.chatbot_draft_section_4_title, R.string.chatbot_draft_section_4_body),
                ChatbotDraftSection(R.string.chatbot_draft_section_5_title, R.string.chatbot_draft_section_5_body)
            )
        ),
        ChatbotDraftPage(
            sections = listOf(
                ChatbotDraftSection(R.string.chatbot_draft_section_6_title, R.string.chatbot_draft_section_6_body)
            )
        ),
        ChatbotDraftPage(
            sections = listOf(
                ChatbotDraftSection(R.string.chatbot_draft_section_7_title, R.string.chatbot_draft_section_7_body)
            )
        ),
        ChatbotDraftPage(
            sections = listOf(
                ChatbotDraftSection(R.string.chatbot_draft_section_8_title, R.string.chatbot_draft_section_8_body),
                ChatbotDraftSection(R.string.chatbot_draft_section_9_title, R.string.chatbot_draft_section_9_body)
            )
        )
    )

    private fun savedContentPages(content: String) = listOf(
        ChatbotDraftPage(
            headingRes = R.string.chatbot_draft_contract_heading,
            sections = listOf(ChatbotDraftSection(R.string.chatbot_draft_saved_content_title, R.string.chatbot_draft_section_1_body, content))
        )
    )

    private fun draftContent(version: String): String {
        val manualBody = intent.getStringExtra(EXTRA_MANUAL_BODY)
        val stage = DraftStage.fromLegacyKey(version) ?: DraftStage.ORIGINAL
        return DraftContentFactory.create(this, stage, manualBody)
    }

    companion object {
        private const val EXTRA_VERSION = "chatbot_draft_version"
        const val EXTRA_PREVIEW_REGENERATION_ERROR = "chatbot_draft_preview_regeneration_error"
        const val EXTRA_PREVIEW_AI_ERROR = "chatbot_draft_preview_ai_error"
        const val EXTRA_PREVIEW_APPLY_ERROR = "chatbot_draft_preview_apply_error"
        const val EXTRA_MANUAL_BODY = "chatbot_draft_manual_body"
        const val EXTRA_SAVED_CONTENT = "chatbot_draft_saved_content"
        const val EXTRA_DRAFT_ID = "chatbot_draft_id"
        const val EXTRA_CONTRACT_ID = "chatbot_draft_contract_id"
        private const val RESULT_EDIT_REQUEST = 614
        private const val MIN_ZOOM_PERCENT = 75
        private const val MAX_ZOOM_PERCENT = 150

        fun createIntent(
            context: Context,
            version: String = ChatbotDraftUiState.DRAFT_VERSION,
            previewRegenerationError: Boolean = false,
            manualBody: String? = null,
            savedContent: String? = null,
            contractId: String? = null,
            draftId: String? = null
        ): Intent = Intent(context, ChatbotDraftPreviewActivity::class.java)
            .putExtra(EXTRA_VERSION, version)
            .putExtra(EXTRA_PREVIEW_REGENERATION_ERROR, previewRegenerationError)
            .putExtra(EXTRA_MANUAL_BODY, manualBody)
            .putExtra(EXTRA_SAVED_CONTENT, savedContent)
            .putExtra(EXTRA_CONTRACT_ID, contractId)
            .putExtra(EXTRA_DRAFT_ID, draftId)
    }
}
