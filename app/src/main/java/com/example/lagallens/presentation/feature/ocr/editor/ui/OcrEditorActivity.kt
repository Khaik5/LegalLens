package com.example.lagallens.presentation.feature.ocr.editor.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityOcrEditorBinding
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import com.example.lagallens.data.datasource.local.OcrEditDraftStore
import com.example.lagallens.presentation.feature.ocr.OcrTextHandoff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import com.example.lagallens.presentation.feature.ocr.editor.contract.OcrEditorUiEffect
import com.example.lagallens.presentation.feature.ocr.editor.contract.OcrEditorUiEvent
import com.example.lagallens.presentation.feature.ocr.editor.contract.OcrEditorUiState
import com.example.lagallens.presentation.feature.ocr.editor.viewmodel.OcrEditorViewModel
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class OcrEditorActivity : com.example.lagallens.presentation.feature.ocr.OcrSessionActivity() {
    private lateinit var binding: ActivityOcrEditorBinding
    private val viewModel by viewModels<OcrEditorViewModel>()
    private val drafts by lazy { OcrEditDraftStore(applicationContext) }
    private val documentId get() = intent.getStringExtra(EXTRA_DOCUMENT_ID).orEmpty()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isFinishing || !isOcrSessionValid()) { finish(); return }
        // The text is kept in memory (see OcrTextHandoff) and is gone after process death. The edit in progress was
        // kept in the session cache, so editing resumes from it; with neither there is nothing to open.
        val handedOver = OcrTextHandoff.peek(intent.getStringExtra(EXTRA_INITIAL_TEXT_TOKEN))
        val resumed = if (handedOver == null && documentId.isNotBlank()) {
            drafts.load(OcrEditDraftStore.PURPOSE_EDITOR, ownerSession, documentId)
        } else null
        val initial = when {
            handedOver != null -> OcrEditDraftStore.Draft(handedOver, handedOver)
            resumed != null -> resumed
            else -> { finish(); return }
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityOcrEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel.onEvent(OcrEditorUiEvent.Initialize(initial.original, initial.edited))
        ViewCompat.setOnApplyWindowInsetsListener(binding.ocrEditorRoot) { view, insets ->
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
        ViewCompat.requestApplyInsets(binding.ocrEditorRoot)
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = viewModel.onEvent(OcrEditorUiEvent.BackClicked)
        })
        bindEvents()
        observeViewModel()
    }

    private fun bindEvents() = with(binding) {
        listOf(ocrEditorBack, ocrEditorRestore, ocrEditorSave, ocrEditorBold, ocrEditorItalic, ocrEditorList).forEach { it.bindPressFeedback() }
        ocrEditorBack.setOnClickListener { viewModel.onEvent(OcrEditorUiEvent.BackClicked) }
        ocrEditorRestore.setOnClickListener { viewModel.onEvent(OcrEditorUiEvent.RestoreOriginalClicked) }
        ocrEditorSave.setOnClickListener { viewModel.onEvent(OcrEditorUiEvent.SaveClicked) }
        listOf(ocrEditorBold, ocrEditorItalic).forEach {
            it.isEnabled = false
            it.alpha = 0.38f
            it.contentDescription = getString(R.string.editor_formatting_unavailable)
        }
        ocrEditorList.setOnClickListener { insertBullet() }
        ocrEditorText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.onEvent(OcrEditorUiEvent.TextChanged(s?.toString().orEmpty()))
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.uiEffect.collect(::handleEffect) }
                launch { viewModel.uiState.debounce(DRAFT_SAVE_DELAY_MILLIS).collect(::persistDraft) }
            }
        }
    }

    /** Keeps the edit in progress, or removes the draft when the text is back to the original. */
    private fun persistDraft(state: OcrEditorUiState) {
        val session = ownerSession
        val document = documentId
        if (isFinishing || document.isBlank() || !isOcrSessionValid()) return
        lifecycleScope.launch(Dispatchers.IO) {
            if (state.text == state.originalText) drafts.clear(OcrEditDraftStore.PURPOSE_EDITOR, session, document)
            else drafts.save(OcrEditDraftStore.PURPOSE_EDITOR, session, document, OcrEditDraftStore.Draft(state.originalText, state.text))
        }
    }

    // The debounce above stops with the screen; the last characters typed must not wait for it.
    override fun onStop() {
        if (::binding.isInitialized) persistDraft(viewModel.uiState.value)
        super.onStop()
    }

    private fun render(state: OcrEditorUiState) = with(binding) {
        if (!isOcrSessionValid()) { root.visibility = android.view.View.INVISIBLE; return@with }
        ocrEditorRevision.setText(
            if (state.isOriginalText) R.string.ocr_editor_original_revision else R.string.ocr_editor_edited_revision
        )
        if (ocrEditorText.text.toString() != state.text) {
            val selectionStart = ocrEditorText.selectionStart.coerceIn(0, state.text.length)
            val selectionEnd = ocrEditorText.selectionEnd.coerceIn(0, state.text.length)
            val wasFocused = ocrEditorText.hasFocus()
            ocrEditorText.setText(state.text)
            if (wasFocused) ocrEditorText.setSelection(selectionStart, selectionEnd)
            else ocrEditorText.setSelection(state.text.length)
        }
    }

    private fun handleEffect(effect: OcrEditorUiEffect) {
        if (!isOcrSessionValid()) return
        when (effect) {
            OcrEditorUiEffect.NavigateBack -> finish()
            OcrEditorUiEffect.ConfirmDiscard -> androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.ocr_editor_discard_title)
                .setMessage(R.string.ocr_editor_discard_body)
                .setPositiveButton(R.string.ocr_editor_discard_action) { _, _ ->
                    viewModel.onEvent(OcrEditorUiEvent.DiscardConfirmed)
                }
                .setNegativeButton(R.string.ocr_editor_discard_keep_editing, null)
                .show()
            is OcrEditorUiEffect.FinishWithSavedText -> {
                setResult(RESULT_OK, Intent().putExtra(EXTRA_EDITED_TEXT_TOKEN, OcrTextHandoff.put(effect.text)))
                finish()
            }
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            OcrTextHandoff.release(intent.getStringExtra(EXTRA_INITIAL_TEXT_TOKEN))
            // Saved, discarded or closed: the edit is no longer in progress.
            if (documentId.isNotBlank()) drafts.clear(OcrEditDraftStore.PURPOSE_EDITOR, ownerSession, documentId)
        }
        super.onDestroy()
    }

    private fun insertBullet() = with(binding.ocrEditorText) {
        text.insert(selectionStart.coerceAtLeast(0), BULLET_PREFIX)
    }

    companion object {
        private const val BULLET_PREFIX = "• "
        const val EXTRA_EDITED_TEXT_TOKEN = "extra_edited_text_token"
        private const val EXTRA_INITIAL_TEXT_TOKEN = "extra_initial_text_token"
        private const val EXTRA_DOCUMENT_ID = "extra_document_id"
        private const val DRAFT_SAVE_DELAY_MILLIS = 400L

        /** [documentId] names the edit in the session cache; without it the edit is not kept across process death. */
        fun createIntent(context: Context, originalText: String, documentId: String = ""): Intent {
            return com.example.lagallens.presentation.feature.ocr.OcrSession.stamp(context, Intent(context, OcrEditorActivity::class.java)
                .putExtra(EXTRA_INITIAL_TEXT_TOKEN, OcrTextHandoff.put(originalText))
                .putExtra(EXTRA_DOCUMENT_ID, documentId))
        }
    }
}
