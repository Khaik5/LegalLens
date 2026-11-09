package com.example.lagallens.presentation.feature.chatbot.draft.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityCompareDraftVersionsBinding
import com.example.lagallens.presentation.feature.chatbot.draft.contract.CompareDraftVersionsUiEvent
import com.example.lagallens.presentation.feature.chatbot.draft.contract.CompareDraftVersionsUiState
import com.example.lagallens.presentation.feature.chatbot.draft.viewmodel.CompareDraftVersionsViewModel
import kotlinx.coroutines.launch
import com.example.lagallens.presentation.common.utils.applySystemBarInsets

class CompareDraftVersionsActivity : DraftSessionActivity() {
    private lateinit var binding: ActivityCompareDraftVersionsBinding
    private val viewModel by viewModels<CompareDraftVersionsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!guardDraftCreate()) return
        useEdgeToEdgeSystemBars()
        binding = ActivityCompareDraftVersionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        binding.compareDraftBack.setOnClickListener { finish() }
        binding.compareDraftReturn.setOnClickListener { finish() }
        viewModel.onEvent(CompareDraftVersionsUiEvent.Opened(intent.getStringExtra(EXTRA_CONTRACT_ID)))
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.uiState.collect(::render) }
        }
    }

    private fun render(state: CompareDraftVersionsUiState) = with(binding) {
        if (state.isUnavailable) {
            android.widget.Toast.makeText(this@CompareDraftVersionsActivity, R.string.chatbot_draft_load_failed, android.widget.Toast.LENGTH_LONG).show()
            finish()
            return@with
        }
        state.clause?.let { compareDraftClause.text = it }
        compareDraftBefore.text = state.before
        compareDraftAfter.text = state.after
    }

    companion object {
        private const val EXTRA_CONTRACT_ID = "chatbot_compare_versions_contract_id"
        fun createIntent(context: Context, contractId: String? = null) = Intent(context, CompareDraftVersionsActivity::class.java)
            .putExtra(EXTRA_CONTRACT_ID, contractId)
            .also { DraftSessionGuard.stamp(context, it) }
    }
}
