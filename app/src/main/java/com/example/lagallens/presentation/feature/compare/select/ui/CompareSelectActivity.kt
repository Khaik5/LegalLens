package com.example.lagallens.presentation.feature.compare.select.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.WindowCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lagallens.R
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import com.example.lagallens.databinding.ActivityCompareSelectBinding
import com.example.lagallens.presentation.common.utils.applySystemBarInsets
import com.example.lagallens.presentation.feature.compare.result.ui.CompareResultActivity
import com.example.lagallens.presentation.feature.compare.select.adapter.CompareContractAdapter
import com.example.lagallens.presentation.feature.compare.select.contract.CompareContract
import com.example.lagallens.presentation.feature.compare.select.contract.CompareSelectUiEffect
import com.example.lagallens.presentation.feature.compare.select.contract.CompareSelectUiEvent
import com.example.lagallens.presentation.feature.compare.select.contract.CompareSelectUiState
import com.example.lagallens.presentation.feature.compare.select.viewmodel.CompareSelectViewModel
import kotlinx.coroutines.launch

class CompareSelectActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCompareSelectBinding
    private val viewModel by viewModels<CompareSelectViewModel>()
    private val adapter = CompareContractAdapter { viewModel.onEvent(CompareSelectUiEvent.ContractClicked(it)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityCompareSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        binding.compareSelectList.layoutManager = LinearLayoutManager(this)
        binding.compareSelectList.adapter = adapter
        binding.compareSelectSubmit.bindPressFeedback()
        binding.compareSelectBack.setOnClickListener { viewModel.onEvent(CompareSelectUiEvent.BackClicked) }
        binding.compareSelectSubmit.setOnClickListener { viewModel.onEvent(CompareSelectUiEvent.CompareClicked) }
        binding.compareSelectSearch.doAfterTextChanged { viewModel.onEvent(CompareSelectUiEvent.QueryChanged(it?.toString().orEmpty())) }
        val initialId = intent.getStringExtra(EXTRA_CONTRACT_ID)
        viewModel.onEvent(CompareSelectUiEvent.Initialize(initialId))
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch { viewModel.uiState.collect(::render) }
            launch { viewModel.uiEffect.collect(::handleEffect) }
        } }
    }

    private fun render(state: CompareSelectUiState) = with(binding) {
        adapter.submitState(state)
        val count = state.selectedIds.size
        compareSelectCount.text = getString(R.string.compare_selected_count, count)
        compareSelectSubmit.isEnabled = count == 2
        compareSelectHint.setText(when (count) { 0 -> R.string.compare_select_two; 1 -> R.string.compare_select_one; else -> R.string.compare_can_change })
        compareSelectNotice.isVisible = count == 2 || (count == 1 && state.initialContractId in state.selectedIds)
        compareSelectNotice.setBackgroundResource(if (count == 2) R.drawable.bg_compare_warning else R.drawable.bg_compare_notice)
        compareSelectNotice.setText(if (count == 2) R.string.compare_limit else R.string.compare_preselected)
        compareSelectEmpty.isVisible = state.contracts.isEmpty()
    }

    private fun handleEffect(effect: CompareSelectUiEffect) {
        when (effect) {
            CompareSelectUiEffect.NavigateBack -> finish()
            is CompareSelectUiEffect.NavigateToResult -> startActivity(CompareResultActivity.createIntent(this, effect.first, effect.second))
        }
    }

    override fun onResume() { super.onResume(); viewModel.onEvent(CompareSelectUiEvent.ScreenResumed) }
    override fun onDestroy() { binding.compareSelectList.adapter = null; super.onDestroy() }

    companion object {
        private const val EXTRA_CONTRACT_ID = "compare_initial_contract_id"
        fun createIntent(context: Context, contractId: String? = null): Intent = Intent(context, CompareSelectActivity::class.java).putExtra(EXTRA_CONTRACT_ID, contractId)
    }
}
