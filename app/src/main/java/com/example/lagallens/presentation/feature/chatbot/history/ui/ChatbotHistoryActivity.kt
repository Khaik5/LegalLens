package com.example.lagallens.presentation.feature.chatbot.history.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityChatbotHistoryBinding
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import com.example.lagallens.presentation.feature.chatbot.history.adapter.ChatbotHistoryAdapter
import com.example.lagallens.presentation.feature.chatbot.history.contract.ChatbotHistoryUiEffect
import com.example.lagallens.presentation.feature.chatbot.history.contract.ChatbotHistoryUiEvent
import com.example.lagallens.presentation.feature.chatbot.history.contract.ChatbotHistoryUiState
import com.example.lagallens.presentation.feature.chatbot.history.viewmodel.ChatbotHistoryViewModel
import com.example.lagallens.presentation.feature.chatbot.ui.ChatbotActivity
import com.example.lagallens.LegalLensApplication
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.launch

class ChatbotHistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatbotHistoryBinding
    private val viewModel by viewModels<ChatbotHistoryViewModel> {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatbotHistoryViewModel((application as LegalLensApplication).chatbotRepository) as T
        }
    }
    private val historyAdapter = ChatbotHistoryAdapter { viewModel.onEvent(ChatbotHistoryUiEvent.ConversationClicked(it)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityChatbotHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.chatbotHistoryRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(binding.chatbotHistoryRoot)
        bindViews()
        observeViewModel()
        viewModel.onEvent(ChatbotHistoryUiEvent.ScreenOpened())
    }

    private fun bindViews() = with(binding) {
        listOf(chatbotHistoryBack, chatbotHistorySearch, chatbotHistoryNewChat).forEach { it.bindPressFeedback() }
        chatbotHistoryList.apply {
            layoutManager = LinearLayoutManager(this@ChatbotHistoryActivity)
            adapter = historyAdapter
        }
        chatbotHistoryBack.setOnClickListener { viewModel.onEvent(ChatbotHistoryUiEvent.BackClicked) }
        chatbotHistorySearch.setOnClickListener { viewModel.onEvent(ChatbotHistoryUiEvent.SearchClicked) }
        chatbotHistoryNewChat.setOnClickListener {
            startActivity(
                ChatbotActivity.createNewConversationIntent(
                    this@ChatbotHistoryActivity,
                    getString(R.string.chatbot_default_contract_name),
                    getString(R.string.chatbot_default_contract_type)
                )
            )
        }
        chatbotHistoryQuery.doAfterTextChanged { viewModel.onEvent(ChatbotHistoryUiEvent.QueryChanged(it?.toString().orEmpty())) }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.uiEffect.collect(::handleEffect) }
            }
        }
    }

    private fun render(state: ChatbotHistoryUiState) {
        historyAdapter.submitList(state.conversations)
        val shouldFocusSearch = state.isSearchVisible && !binding.chatbotHistoryQuery.isVisible
        binding.chatbotHistoryQuery.isVisible = state.isSearchVisible
        binding.chatbotHistoryTodayLabel.isVisible = !state.isSearchVisible
        binding.chatbotHistoryFooter.isVisible = !state.isSearchVisible
        binding.chatbotHistoryPreservationNotice.isVisible = !state.isSearchVisible
        binding.chatbotHistoryEmpty.isVisible = state.conversations.isEmpty()
        if (binding.chatbotHistoryQuery.text.toString() != state.query) binding.chatbotHistoryQuery.setText(state.query)
        if (shouldFocusSearch) {
            binding.chatbotHistoryQuery.requestFocus()
            binding.chatbotHistoryQuery.post {
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showSoftInput(binding.chatbotHistoryQuery, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    private fun handleEffect(effect: ChatbotHistoryUiEffect) {
        when (effect) {
            ChatbotHistoryUiEffect.NavigateBack -> finish()
            is ChatbotHistoryUiEffect.OpenConversation -> startActivity(
                if (effect.conversation.isDraftRequest) {
                    ChatbotActivity.createDraftContinuationIntent(
                        this,
                        effect.conversation.contractName,
                        getString(R.string.chatbot_default_contract_type)
                    )
                } else {
                    ChatbotActivity.createHistoryIntent(
                        this,
                        effect.conversation.contractName,
                        effect.conversation.id,
                        effect.conversation.contractId
                    )
                }
            )
            is ChatbotHistoryUiEffect.ShowMessage -> android.widget.Toast.makeText(
                this,
                effect.messageRes,
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroy() {
        binding.chatbotHistoryList.adapter = null
        super.onDestroy()
    }

    companion object {
        fun createIntent(context: Context): Intent = Intent(context, ChatbotHistoryActivity::class.java)
    }

}
