package com.example.lagallens.presentation.feature.chatbot.history.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityChatbotHistoryBinding
import com.example.lagallens.presentation.feature.chatbot.history.adapter.ChatbotHistoryAdapter
import com.example.lagallens.presentation.feature.chatbot.history.contract.ChatbotHistoryConversation
import com.example.lagallens.presentation.feature.chatbot.history.contract.ChatbotHistoryUiEffect
import com.example.lagallens.presentation.feature.chatbot.history.contract.ChatbotHistoryUiEvent
import com.example.lagallens.presentation.feature.chatbot.history.contract.ChatbotHistoryUiState
import com.example.lagallens.presentation.feature.chatbot.history.viewmodel.ChatbotHistoryViewModel
import kotlinx.coroutines.launch

class ChatbotHistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatbotHistoryBinding
    private val viewModel by viewModels<ChatbotHistoryViewModel>()
    private val historyAdapter = ChatbotHistoryAdapter()

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
        viewModel.onEvent(ChatbotHistoryUiEvent.ScreenOpened(historyConversations()))
    }

    private fun bindViews() = with(binding) {
        chatbotHistoryList.apply {
            layoutManager = LinearLayoutManager(this@ChatbotHistoryActivity)
            adapter = historyAdapter
        }
        chatbotHistoryBack.setOnClickListener { viewModel.onEvent(ChatbotHistoryUiEvent.BackClicked) }
        chatbotHistorySearch.setOnClickListener { viewModel.onEvent(ChatbotHistoryUiEvent.SearchClicked) }
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
    }

    private fun handleEffect(effect: ChatbotHistoryUiEffect) {
        when (effect) {
            ChatbotHistoryUiEffect.NavigateBack -> finish()
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

    private fun historyConversations(): List<ChatbotHistoryConversation> = listOf(
        ChatbotHistoryConversation(
            id = 1L,
            contractName = getString(R.string.chatbot_history_first_title),
            timestamp = getString(R.string.chatbot_history_first_timestamp),
            messageCount = getString(R.string.chatbot_history_first_count),
            preview = getString(R.string.chatbot_history_first_preview)
        ),
        ChatbotHistoryConversation(
            id = 2L,
            contractName = getString(R.string.chatbot_history_second_title),
            timestamp = getString(R.string.chatbot_history_second_timestamp),
            messageCount = getString(R.string.chatbot_history_second_count),
            preview = getString(R.string.chatbot_history_second_preview)
        ),
        ChatbotHistoryConversation(
            id = 3L,
            contractName = getString(R.string.chatbot_history_third_title),
            timestamp = getString(R.string.chatbot_history_third_timestamp),
            messageCount = getString(R.string.chatbot_history_third_count),
            preview = getString(R.string.chatbot_history_third_preview)
        )
    )
}
