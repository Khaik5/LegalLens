package com.example.lagallens.presentation.feature.chatbot.draft.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.BuildConfig
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityChatbotDraftVersionsBinding
import com.example.lagallens.presentation.common.utils.applySystemBarInsets
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftUiState
import com.example.lagallens.domain.model.DraftStage
import com.example.lagallens.presentation.feature.documentviewer.ui.DocumentViewerActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatbotDraftVersionsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatbotDraftVersionsBinding
    private val hasD3 by lazy { intent.getBooleanExtra(EXTRA_HAS_D3, false) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        binding = ActivityChatbotDraftVersionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        binding.draftVersionsBack.setOnClickListener { finish() }
        binding.draftVersionD2View.setOnClickListener { startActivity(AppliedDraftPreviewActivity.createIntent(this, hasD3)) }
        binding.draftVersionD1View.setOnClickListener { startActivity(ChatbotDraftPreviewActivity.createIntent(this, ChatbotDraftUiState.DRAFT_VERSION)) }
        binding.draftVersionD1Copy.setOnClickListener { confirmCopyD1() }
        binding.draftVersionOriginalView.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val result = withContext(Dispatchers.IO) {
                        val app = application as LegalLensApplication
                        val draft = app.draftRepository.getLatestDraft(DraftStage.ORIGINAL)
                        val contract = draft?.contractId?.let { app.contractRepository.getContract(it) }
                        val document = contract?.let { app.documentRepository.getDocument(it.documentId) }
                        contract to (contract?.fileUri ?: document?.uri)
                    }
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
                    android.widget.Toast.makeText(this@ChatbotDraftVersionsActivity, R.string.contract_detail_feature_unavailable, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.draftVersionsCompare.setOnClickListener { startActivity(CompareDraftVersionsActivity.createIntent(this)) }
        lifecycleScope.launch {
            val storedCopy = try {
                withContext(Dispatchers.IO) { (application as LegalLensApplication).draftRepository.getLatestDraft(DraftStage.COPY) != null }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Exception) {
                false
            }
            binding.draftVersionD3Card.isVisible = hasD3 || storedCopy
        }
        binding.draftVersionD3View.setOnClickListener { startActivity(ChatbotDraftPreviewActivity.createIntent(this, ChatbotDraftUiState.COPY_VERSION)) }
    }

    private fun confirmCopyD1() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.chatbot_draft_copy_confirm_title)
            .setMessage(R.string.chatbot_draft_copy_confirm_body)
            .setNegativeButton(R.string.chatbot_draft_ai_cancel, null)
            .setPositiveButton(R.string.chatbot_draft_copy_confirm_action) { _, _ ->
                startActivity(CopyDraftVersionActivity.createIntent(this, BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_PREVIEW_COPY_ERROR, false)))
            }
            .show()
    }

    companion object {
        private const val EXTRA_HAS_D3 = "chatbot_draft_versions_has_d3"
        const val EXTRA_PREVIEW_COPY_ERROR = "chatbot_draft_versions_copy_error"
        fun createIntent(context: Context, hasD3: Boolean = false, previewCopyError: Boolean = false) =
            Intent(context, ChatbotDraftVersionsActivity::class.java)
                .putExtra(EXTRA_HAS_D3, hasD3)
                .putExtra(EXTRA_PREVIEW_COPY_ERROR, previewCopyError)
    }
}
