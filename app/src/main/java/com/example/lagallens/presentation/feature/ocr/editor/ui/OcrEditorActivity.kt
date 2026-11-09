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
import com.example.lagallens.presentation.feature.ocr.editor.contract.OcrEditorUiEffect
import com.example.lagallens.presentation.feature.ocr.editor.contract.OcrEditorUiEvent
import com.example.lagallens.presentation.feature.ocr.editor.contract.OcrEditorUiState
import com.example.lagallens.presentation.feature.ocr.editor.viewmodel.OcrEditorViewModel
import kotlinx.coroutines.launch

class OcrEditorActivity : com.example.lagallens.presentation.feature.ocr.OcrSessionActivity() {
    private lateinit var binding: ActivityOcrEditorBinding
    private val viewModel by viewModels<OcrEditorViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isFinishing || !isOcrSessionValid()) { finish(); return }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityOcrEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel.onEvent(OcrEditorUiEvent.Initialize(intent.getStringExtra(EXTRA_INITIAL_TEXT).orEmpty()))
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
            }
        }
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
            is OcrEditorUiEffect.FinishWithSavedText -> {
                setResult(RESULT_OK, Intent().putExtra(EXTRA_EDITED_TEXT, effect.text))
                finish()
            }
        }
    }

    private fun insertBullet() = with(binding.ocrEditorText) {
        text.insert(selectionStart.coerceAtLeast(0), BULLET_PREFIX)
    }

    companion object {
        private const val BULLET_PREFIX = "• "
        const val EXTRA_EDITED_TEXT = "extra_edited_text"
        private const val EXTRA_INITIAL_TEXT = "extra_initial_text"

        fun createIntent(context: Context, originalText: String): Intent {
            return com.example.lagallens.presentation.feature.ocr.OcrSession.stamp(context, Intent(context, OcrEditorActivity::class.java)
                .putExtra(EXTRA_INITIAL_TEXT, originalText))
        }
    }
}
