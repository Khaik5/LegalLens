package com.example.lagallens.presentation.feature.creditstore.flow.ui

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEffect
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEvent
import com.example.lagallens.presentation.feature.creditstore.flow.viewmodel.CreditFlowViewModel
import kotlinx.coroutines.launch
import com.example.lagallens.R

abstract class CreditFlowActivity : AppCompatActivity() {
    protected val viewModel by viewModels<CreditFlowViewModel>()

    override fun setContentView(view: View?) {
        super.setContentView(view)
        view?.let {
            clearButtonBackgroundTints(it)
            applyActionContainerBackgrounds(it)
        }
    }

    protected fun collectEffects(handleEffect: (CreditFlowUiEffect) -> Unit) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEffect.collect(handleEffect)
            }
        }
    }

    protected fun applySystemBarInsets(root: View) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    protected fun bindSystemBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                viewModel.onEvent(CreditFlowUiEvent.BackClicked)
            }
        })
    }

    private fun clearButtonBackgroundTints(view: View) {
        if (view is Button) {
            view.backgroundTintList = null
            ViewCompat.setBackgroundTintList(view, null)
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                clearButtonBackgroundTints(view.getChildAt(index))
            }
        }
    }

    private fun applyActionContainerBackgrounds(view: View) {
        if (view is ViewGroup) {
            val entryName = runCatching { resources.getResourceEntryName(view.id) }.getOrNull()
            if (entryName?.endsWith("Actions") == true) {
                view.setBackgroundColor(ContextCompat.getColor(this, R.color.legal_lens_background))
            }
            for (index in 0 until view.childCount) {
                applyActionContainerBackgrounds(view.getChildAt(index))
            }
        }
    }
}
