package com.example.lagallens.presentation.feature.chatbot.draft.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityCopyDraftVersionBinding
import com.example.lagallens.presentation.common.utils.applySystemBarInsets
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftUiState
import com.example.lagallens.presentation.feature.chatbot.draft.contract.CopyDraftVersionStatus
import com.example.lagallens.presentation.feature.chatbot.draft.contract.CopyDraftVersionUiEvent
import com.example.lagallens.presentation.feature.chatbot.draft.viewmodel.CopyDraftVersionViewModel
import kotlinx.coroutines.launch

class CopyDraftVersionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCopyDraftVersionBinding
    private val viewModel by viewModels<CopyDraftVersionViewModel>()
    private var openingDraft = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        binding = ActivityCopyDraftVersionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        binding.copyDraftBack.setOnClickListener { finish() }
        binding.copyDraftRetry.setOnClickListener { viewModel.onEvent(CopyDraftVersionUiEvent.RetryClicked) }
        binding.copyDraftReturn.setOnClickListener { finish() }
        binding.copyDraftView.setOnClickListener {
            if (openingDraft) return@setOnClickListener
            openingDraft = true
            val draftId = viewModel.uiState.value.draftId
            if (draftId == null) {
                openingDraft = false
                return@setOnClickListener
            }
            startActivity(ChatbotDraftPreviewActivity.createIntent(this@CopyDraftVersionActivity, ChatbotDraftUiState.COPY_VERSION, draftId = draftId))
        }
        binding.copyDraftManage.setOnClickListener { startActivity(ChatbotDraftVersionsActivity.createIntent(this, hasD3 = true)) }
        viewModel.onEvent(CopyDraftVersionUiEvent.Opened(intent.getBooleanExtra(EXTRA_PREVIEW_FAILURE, false), null, null))
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.uiState.collect(::render) }
        }
    }

    private fun render(state: com.example.lagallens.presentation.feature.chatbot.draft.contract.CopyDraftVersionUiState) = with(binding) {
        val loading = state.status == CopyDraftVersionStatus.LOADING
        val error = state.status == CopyDraftVersionStatus.ERROR
        val success = state.status == CopyDraftVersionStatus.SUCCESS
        copyDraftLoading.isVisible = loading
        copyDraftError.isVisible = error
        copyDraftSuccess.isVisible = success
        copyDraftPending.isVisible = loading
        copyDraftRetry.isVisible = error
        copyDraftReturn.isVisible = error
        copyDraftView.isVisible = success
        copyDraftManage.isVisible = success
    }

    companion object {
        private const val EXTRA_PREVIEW_FAILURE = "chatbot_copy_draft_preview_failure"
        fun createIntent(context: Context, previewFailure: Boolean = false) =
            Intent(context, CopyDraftVersionActivity::class.java).putExtra(EXTRA_PREVIEW_FAILURE, previewFailure)
    }
}
