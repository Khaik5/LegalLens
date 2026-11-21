package com.example.lagallens.presentation.feature.chatbot.draft.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.databinding.ActivityChatbotDraftListBinding
import com.example.lagallens.presentation.common.utils.applySystemBarInsets
import com.example.lagallens.presentation.feature.chatbot.draft.adapter.ChatbotDraftListAdapter
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftListStatus
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftListUiEffect
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftListUiEvent
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftListUiState
import com.example.lagallens.presentation.feature.chatbot.draft.viewmodel.ChatbotDraftListViewModel
import kotlinx.coroutines.launch

/** The user's drafts, opened from the profile; a row opens the draft preview. */
class ChatbotDraftListActivity : DraftSessionActivity() {
    private lateinit var binding: ActivityChatbotDraftListBinding
    private val viewModel by viewModels<ChatbotDraftListViewModel> {
        val app = application as LegalLensApplication
        val owner = DraftSessionGuard.owner(intent)
        ChatbotDraftListViewModel.Factory(owner, app::currentSessionKey) { app.draftRepository.forSession(owner) }
    }
    private val adapter = ChatbotDraftListAdapter { viewModel.onEvent(ChatbotDraftListUiEvent.ItemClicked(it)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!guardDraftCreate()) return
        useEdgeToEdgeSystemBars()
        binding = ActivityChatbotDraftListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        binding.draftListRecycler.layoutManager = LinearLayoutManager(this)
        binding.draftListRecycler.adapter = adapter
        binding.draftListBack.setOnClickListener { finish() }
        binding.draftListRetry.setOnClickListener { viewModel.onEvent(ChatbotDraftListUiEvent.RetryClicked) }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { if (isDraftSessionValid()) render(it) } }
                launch { viewModel.uiEffect.collect { if (isDraftSessionValid()) handleEffect(it) } }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Reloaded every time the screen returns, so a draft saved or edited meanwhile is current.
        if (isDraftSessionValid()) viewModel.onEvent(ChatbotDraftListUiEvent.Opened)
    }

    private fun render(state: ChatbotDraftListUiState) = with(binding) {
        draftListLoading.isVisible = state.status == ChatbotDraftListStatus.LOADING
        draftListEmpty.isVisible = state.status == ChatbotDraftListStatus.EMPTY
        draftListError.isVisible = state.status == ChatbotDraftListStatus.ERROR
        draftListRecycler.isVisible = state.status == ChatbotDraftListStatus.CONTENT
        adapter.submitList(state.items)
    }

    private fun handleEffect(effect: ChatbotDraftListUiEffect) {
        when (effect) {
            is ChatbotDraftListUiEffect.OpenDraft -> startActivity(
                ChatbotDraftPreviewActivity.createIntent(this, draftId = effect.draftId, contractId = effect.contractId)
            )
        }
    }

    companion object {
        fun createIntent(context: Context): Intent = Intent(context, ChatbotDraftListActivity::class.java)
            .also { DraftSessionGuard.stamp(context, it) }
    }
}
