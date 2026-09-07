package com.example.lagallens.presentation.feature.ocr.ui

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.StyleSpan
import android.widget.Toast
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
import com.example.lagallens.databinding.ActivityOcrReviewBinding
import com.example.lagallens.presentation.feature.ocr.contract.OcrReviewEffect
import com.example.lagallens.presentation.feature.ocr.contract.OcrReviewEvent
import com.example.lagallens.presentation.feature.ocr.contract.OcrReviewUiState
import com.example.lagallens.presentation.feature.ocr.viewmodel.OcrReviewViewModel
import kotlinx.coroutines.launch

class OcrReviewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOcrReviewBinding
    private val viewModel by viewModels<OcrReviewViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityOcrReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.ocrReviewRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
        bindEvents()
        observeViewModel()
    }

    private fun bindEvents() = with(binding) {
        ocrReviewBack.setOnClickListener { viewModel.onEvent(OcrReviewEvent.BackClicked) }
        ocrReviewRestore.setOnClickListener { viewModel.onEvent(OcrReviewEvent.RestoreOriginalClicked) }
        ocrReviewSave.setOnClickListener { viewModel.onEvent(OcrReviewEvent.SaveClicked) }
        ocrReviewBold.setOnClickListener { applyStyle(Typeface.BOLD) }
        ocrReviewItalic.setOnClickListener { applyStyle(Typeface.ITALIC) }
        ocrReviewList.setOnClickListener { insertBullet() }
        ocrReviewText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.onEvent(OcrReviewEvent.TextChanged(s?.toString().orEmpty()))
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.effects.collect(::handleEffect) }
            }
        }
    }

    private fun render(state: OcrReviewUiState) = with(binding) {
        ocrReviewRevision.setText(
            if (state.isOriginalText) R.string.ocr_review_original_revision else R.string.ocr_review_edited_revision
        )
        if (ocrReviewText.text.toString() != state.text) {
            ocrReviewText.setText(state.text)
            ocrReviewText.setSelection(state.text.length)
        }
    }

    private fun handleEffect(effect: OcrReviewEffect) {
        when (effect) {
            OcrReviewEffect.NavigateBack -> finish()
            OcrReviewEffect.ShowSaved -> Toast.makeText(this, R.string.ocr_review_saved, Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyStyle(style: Int) = with(binding.ocrReviewText) {
        if (selectionStart != selectionEnd) {
            text.setSpan(StyleSpan(style), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun insertBullet() = with(binding.ocrReviewText) {
        val insertionIndex = selectionStart.coerceAtLeast(0)
        text.insert(insertionIndex, BULLET_PREFIX)
    }

    private companion object {
        const val BULLET_PREFIX = "• "
    }
}
