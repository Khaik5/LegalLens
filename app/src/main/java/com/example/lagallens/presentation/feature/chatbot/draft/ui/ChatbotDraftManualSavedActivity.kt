package com.example.lagallens.presentation.feature.chatbot.draft.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.lagallens.databinding.ActivityChatbotDraftManualSavedBinding
import com.example.lagallens.presentation.common.utils.applySystemBarInsets
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftUiState

class ChatbotDraftManualSavedActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatbotDraftManualSavedBinding
    private val draftId by lazy { intent.getStringExtra(EXTRA_DRAFT_ID).orEmpty() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityChatbotDraftManualSavedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        binding.draftManualSavedBack.setOnClickListener { finish() }
        binding.draftManualSavedView.setOnClickListener {
            startActivity(ChatbotDraftPreviewActivity.createIntent(this, ChatbotDraftUiState.MANUAL_VERSION, draftId = draftId))
            finish()
        }
    }

    companion object {
        private const val EXTRA_DRAFT_ID = "chatbot_draft_manual_saved_draft_id"
        fun createIntent(context: Context, draftId: String) = Intent(context, ChatbotDraftManualSavedActivity::class.java).putExtra(EXTRA_DRAFT_ID, draftId)
    }
}
