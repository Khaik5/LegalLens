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
import android.text.Editable
import android.text.Spannable
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.text.TextWatcher
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityChatbotDraftManualEditBinding
import com.example.lagallens.databinding.DialogChatbotDraftDiscardEditBinding
import com.example.lagallens.presentation.common.utils.applySystemBarInsets
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftManualEditUiEvent
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftManualEditUiEffect
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftUiState
import com.example.lagallens.presentation.feature.chatbot.draft.viewmodel.ChatbotDraftManualEditViewModel
import kotlinx.coroutines.launch

class ChatbotDraftManualEditActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatbotDraftManualEditBinding
    private val viewModel by viewModels<ChatbotDraftManualEditViewModel>()
    private var rendering = false
    private val initialBody by lazy { intent.getStringExtra(EXTRA_BODY) ?: getString(R.string.chatbot_draft_section_2_body) }
    private val initialAddition by lazy {
        intent.getStringExtra(EXTRA_ADDITION) ?: if (intent.hasExtra(EXTRA_BODY)) "" else getString(R.string.chatbot_draft_manual_default_addition)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityChatbotDraftManualEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        bindActions()
        viewModel.onEvent(ChatbotDraftManualEditUiEvent.Opened(initialBody, initialAddition))
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.uiState.collect { state ->
                rendering = true
                if (binding.draftManualClauseBody.text.toString() != state.body) binding.draftManualClauseBody.setText(state.body)
                if (binding.draftManualAddition.text.toString() != state.addition) binding.draftManualAddition.setText(state.addition)
                binding.draftManualSave.isEnabled = !state.isSaving
                binding.draftManualSave.alpha = if (state.isSaving) 0.6f else 1f
                rendering = false
            } }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEffect.collect { effect ->
                    when (effect) {
                        is ChatbotDraftManualEditUiEffect.Saved -> {
                            startActivity(ChatbotDraftManualSavedActivity.createIntent(this@ChatbotDraftManualEditActivity, effect.draftId))
                            finish()
                        }
                        ChatbotDraftManualEditUiEffect.SaveFailed -> Toast.makeText(this@ChatbotDraftManualEditActivity, R.string.chatbot_draft_save_failed, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun bindActions() = with(binding) {
        draftManualBack.setOnClickListener { requestClose() }
        draftManualCancel.setOnClickListener { requestClose() }
        draftManualSave.setOnClickListener {
            viewModel.onEvent(ChatbotDraftManualEditUiEvent.SaveClicked(intent.getStringExtra(EXTRA_SOURCE_DRAFT_ID), intent.getStringExtra(EXTRA_CONTRACT_ID)))
        }
        draftManualSelectAnother.setOnClickListener {
            Toast.makeText(this@ChatbotDraftManualEditActivity, R.string.chatbot_draft_manual_other_clause_unavailable, Toast.LENGTH_SHORT).show()
        }
        draftManualClauseBody.addTextChangedListener(watcher { viewModel.onEvent(ChatbotDraftManualEditUiEvent.BodyChanged(it)) })
        draftManualAddition.addTextChangedListener(watcher { viewModel.onEvent(ChatbotDraftManualEditUiEvent.AdditionChanged(it)) })
        draftFormatList.setOnClickListener {
            val start = draftManualClauseBody.selectionStart.coerceAtLeast(0)
            draftManualClauseBody.text?.insert(start, "• ")
        }
        draftFormatBold.setOnClickListener { toggleFormat(Typeface.BOLD) }
        draftFormatItalic.setOnClickListener { toggleFormat(Typeface.ITALIC) }
    }

    private fun watcher(onChanged: (String) -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (!rendering) onChanged(s?.toString().orEmpty())
        }
        override fun afterTextChanged(s: Editable?) = Unit
    }

    private fun toggleFormat(style: Int) {
        val edit = binding.draftManualClauseBody
        val start = edit.selectionStart.coerceAtLeast(0)
        val end = edit.selectionEnd.coerceAtLeast(start)
        if (start < end) edit.text?.setSpan(StyleSpan(style), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun requestClose() {
        if (!viewModel.uiState.value.isDirty) {
            finish()
            return
        }
        val dialog = Dialog(this)
        val dialogBinding = DialogChatbotDraftDiscardEditBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.38f)
            setLayout(resources.displayMetrics.widthPixels - 2 * resources.getDimensionPixelSize(R.dimen.dp_24), WindowManager.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.CENTER)
        }
        dialogBinding.draftDiscardContinue.setOnClickListener { dialog.dismiss() }
        dialogBinding.draftDiscardConfirm.setOnClickListener { dialog.dismiss(); finish() }
        dialog.show()
        dialog.window?.setLayout(resources.displayMetrics.widthPixels - 2 * resources.getDimensionPixelSize(R.dimen.dp_24), WindowManager.LayoutParams.WRAP_CONTENT)
    }

    companion object {
        private const val EXTRA_BODY = "chatbot_draft_manual_edit_body"
        private const val EXTRA_ADDITION = "chatbot_draft_manual_edit_addition"
        private const val EXTRA_SOURCE_DRAFT_ID = "chatbot_draft_manual_edit_source_draft_id"
        private const val EXTRA_CONTRACT_ID = "chatbot_draft_manual_edit_contract_id"
        fun createIntent(context: Context, body: String? = null, sourceDraftId: String? = null, contractId: String? = null, addition: String? = null) =
            Intent(context, ChatbotDraftManualEditActivity::class.java)
                .putExtra(EXTRA_BODY, body)
                .putExtra(EXTRA_ADDITION, addition)
                .putExtra(EXTRA_SOURCE_DRAFT_ID, sourceDraftId)
                .putExtra(EXTRA_CONTRACT_ID, contractId)
    }
}
