package com.example.lagallens.presentation.feature.chatbot.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
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
import com.example.lagallens.databinding.ActivityChatbotBinding
import com.example.lagallens.presentation.feature.chatbot.adapter.ChatbotMessageAdapter
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotUiEffect
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotUiEvent
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotUiState
import com.example.lagallens.presentation.feature.chatbot.viewmodel.ChatbotViewModel
import kotlinx.coroutines.launch

class ChatbotActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatbotBinding
    private val viewModel by viewModels<ChatbotViewModel>()
    private val messageAdapter = ChatbotMessageAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityChatbotBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.chatbotRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                maxOf(systemBars.bottom, ime.bottom)
            )
            insets
        }
        ViewCompat.requestApplyInsets(binding.chatbotRoot)
        bindViews()
        observeViewModel()
        viewModel.onEvent(
            ChatbotUiEvent.ScreenOpened(
                contractName = intent.getStringExtra(EXTRA_CONTRACT_NAME)
                    .orEmpty()
                    .ifBlank { getString(R.string.contract_office_lease) },
                contractType = intent.getStringExtra(EXTRA_CONTRACT_TYPE)
                    .orEmpty()
                    .ifBlank { getString(R.string.contract_office_lease_subtitle) },
                initialGreeting = getString(R.string.chatbot_initial_greeting),
                initialQuestion = getString(R.string.chatbot_initial_question),
                initialAnswer = getString(R.string.chatbot_initial_answer),
                localResponse = getString(R.string.chatbot_local_response)
            )
        )
    }

    private fun bindViews() = with(binding) {
        chatbotMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatbotActivity)
            adapter = messageAdapter
        }
        chatbotBack.setOnClickListener { viewModel.onEvent(ChatbotUiEvent.BackClicked) }
        chatbotMore.setOnClickListener { viewModel.onEvent(ChatbotUiEvent.AttachmentClicked) }
        chatbotAttach.setOnClickListener { viewModel.onEvent(ChatbotUiEvent.AttachmentClicked) }
        chatbotSend.setOnClickListener { viewModel.onEvent(ChatbotUiEvent.SendClicked) }
        chatbotInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                viewModel.onEvent(ChatbotUiEvent.SendClicked)
                true
            } else {
                false
            }
        }
        chatbotInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.onEvent(ChatbotUiEvent.MessageChanged(s?.toString().orEmpty()))
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.uiEffect.collect(::handleEffect) }
            }
        }
    }

    private fun render(state: ChatbotUiState) = with(binding) {
        chatbotContractName.text = state.contractName
        chatbotContractType.text = state.contractType
        if (chatbotInput.text.toString() != state.messageDraft) {
            chatbotInput.setText(state.messageDraft)
            chatbotInput.setSelection(state.messageDraft.length)
        }
        messageAdapter.submitList(state.messages) {
            if (state.messages.isNotEmpty()) chatbotMessages.scrollToPosition(state.messages.lastIndex)
        }
    }

    private fun handleEffect(effect: ChatbotUiEffect) {
        when (effect) {
            ChatbotUiEffect.NavigateBack -> finish()
            is ChatbotUiEffect.ShowMessage -> android.widget.Toast.makeText(this, effect.messageRes, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        binding.chatbotMessages.adapter = null
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_CONTRACT_NAME = "chatbot_contract_name"
        private const val EXTRA_CONTRACT_TYPE = "chatbot_contract_type"

        fun createIntent(context: Context, contractName: String, contractType: String): Intent =
            Intent(context, ChatbotActivity::class.java)
                .putExtra(EXTRA_CONTRACT_NAME, contractName)
                .putExtra(EXTRA_CONTRACT_TYPE, contractType)
    }
}
