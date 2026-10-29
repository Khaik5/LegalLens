package com.example.lagallens.presentation.feature.chatbot.help.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityChatbotHelpBinding
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import com.example.lagallens.databinding.ItemChatbotHelpFaqBinding
import com.example.lagallens.presentation.common.utils.applySystemBarInsets
import com.example.lagallens.presentation.feature.chatbot.help.viewmodel.ChatbotHelpViewModel
import com.example.lagallens.presentation.feature.chatbot.help.contract.ChatbotHelpUiEvent
import com.example.lagallens.presentation.feature.chatbot.help.contract.ChatbotHelpUiEffect
import kotlinx.coroutines.launch

class ChatbotHelpActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatbotHelpBinding
    private val viewModel by viewModels<ChatbotHelpViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply { isAppearanceLightStatusBars = true; isAppearanceLightNavigationBars = true }
        binding = ActivityChatbotHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        binding.helpBack.bindPressFeedback()
        binding.helpBack.setOnClickListener { viewModel.onEvent(ChatbotHelpUiEvent.BackClicked) }
        binding.helpAttachment.informationCardTitle.setText(R.string.chatbot_help_attachment_title)
        binding.helpAttachment.informationCardBody.setText(R.string.chatbot_help_attachment_body)
        binding.helpAttachment.root.setBackgroundResource(R.drawable.bg_compare_notice)
        listOf(binding.helpQuestionTerm, binding.helpQuestionPayment, binding.helpQuestionObligations, binding.helpQuestionTermination).forEach(::bindSuggestion)
        bindFaq(binding.helpFaqEvidence, 0, R.string.chatbot_help_faq_evidence, R.string.chatbot_help_faq_evidence_answer)
        bindFaq(binding.helpFaqUnreadable, 1, R.string.chatbot_help_faq_unreadable, R.string.chatbot_help_faq_unreadable_answer)
        bindFaq(binding.helpFaqFollowup, 2, R.string.chatbot_help_faq_followup, R.string.chatbot_help_faq_followup_answer)
        bindFaq(binding.helpFaqVerify, 3, R.string.chatbot_help_faq_verify, R.string.chatbot_help_faq_verify_answer)
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch { viewModel.uiState.collect { state ->
                listOf(binding.helpFaqEvidence, binding.helpFaqUnreadable, binding.helpFaqFollowup, binding.helpFaqVerify).forEachIndexed { index, faq ->
                    faq.helpFaqAnswer.isVisible = state.expandedFaq == index
                    faq.helpFaqChevron.rotation = if (state.expandedFaq == index) 180f else 0f
                }
            } }
            launch { viewModel.uiEffect.collect { effect -> when (effect) {
                ChatbotHelpUiEffect.Close -> finish()
                is ChatbotHelpUiEffect.ReturnQuestion -> { setResult(RESULT_OK, Intent().putExtra(EXTRA_SELECTED_QUESTION, effect.question)); finish() }
            } } }
        } }
    }

    private fun bindSuggestion(view: TextView) { view.bindPressFeedback(); view.setOnClickListener { viewModel.onEvent(ChatbotHelpUiEvent.QuestionClicked(view.text.toString())) } }
    private fun bindFaq(item: ItemChatbotHelpFaqBinding, index: Int, question: Int, answer: Int) {
        item.helpFaqQuestion.setText(question)
        item.helpFaqAnswer.setText(answer)
        item.root.bindPressFeedback()
        item.root.setOnClickListener { viewModel.onEvent(ChatbotHelpUiEvent.FaqClicked(index)) }
    }

    companion object {
        const val EXTRA_SELECTED_QUESTION = "chatbot_help_question"
        fun createIntent(context: Context): Intent = Intent(context, ChatbotHelpActivity::class.java)
    }
}
