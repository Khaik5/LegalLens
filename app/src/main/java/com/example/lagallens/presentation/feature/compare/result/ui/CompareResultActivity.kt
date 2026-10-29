package com.example.lagallens.presentation.feature.compare.result.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityCompareResultBinding
import com.example.lagallens.databinding.ItemCompareOverviewBinding
import com.example.lagallens.presentation.common.utils.applySystemBarInsets
import com.example.lagallens.presentation.common.utils.animateContentEntrance
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import com.example.lagallens.presentation.feature.compare.result.contract.CompareResultStatus
import com.example.lagallens.presentation.feature.compare.result.contract.CompareResultTab
import com.example.lagallens.presentation.feature.compare.result.contract.CompareResultUiEffect
import com.example.lagallens.presentation.feature.compare.result.contract.CompareResultUiEvent
import com.example.lagallens.presentation.feature.compare.result.contract.CompareResultUiState
import com.example.lagallens.presentation.feature.compare.result.viewmodel.CompareResultViewModel
import com.example.lagallens.presentation.feature.compare.select.contract.CompareContract
import kotlinx.coroutines.launch

class CompareResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCompareResultBinding
    private val viewModel by viewModels<CompareResultViewModel>()
    private var visibleTab: CompareResultTab? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityCompareResultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        listOf(binding.compareResultBack, binding.compareResultRetry, binding.compareTabOverview,
            binding.compareTabRisk, binding.compareTabClauses, binding.compareShowRisk,
            binding.comparePaymentToggle, binding.compareTermToggle, binding.compareTrialToggle,
            binding.compareCycleToggle).forEach { it.bindPressFeedback() }
        binding.compareResultBack.setOnClickListener { viewModel.onEvent(CompareResultUiEvent.BackClicked) }
        binding.compareResultRetry.setOnClickListener { viewModel.onEvent(CompareResultUiEvent.RetryClicked) }
        binding.compareTabOverview.setOnClickListener { viewModel.onEvent(CompareResultUiEvent.TabClicked(CompareResultTab.OVERVIEW)) }
        binding.compareTabRisk.setOnClickListener { viewModel.onEvent(CompareResultUiEvent.TabClicked(CompareResultTab.RISK)) }
        binding.compareTabClauses.setOnClickListener { viewModel.onEvent(CompareResultUiEvent.TabClicked(CompareResultTab.CLAUSES)) }
        binding.compareShowRisk.setOnClickListener { viewModel.onEvent(CompareResultUiEvent.TabClicked(CompareResultTab.RISK)) }
        binding.comparePaymentToggle.setOnClickListener { viewModel.onEvent(CompareResultUiEvent.SectionClicked("payment")) }
        binding.compareTermToggle.setOnClickListener { viewModel.onEvent(CompareResultUiEvent.SectionClicked("term")) }
        binding.compareTrialToggle.setOnClickListener { viewModel.onEvent(CompareResultUiEvent.SectionClicked("trial")) }
        binding.compareCycleToggle.setOnClickListener { viewModel.onEvent(CompareResultUiEvent.SectionClicked("cycle")) }
        val firstId = intent.getStringExtra(EXTRA_FIRST_ID).orEmpty()
        val secondId = intent.getStringExtra(EXTRA_SECOND_ID).orEmpty()
        if (firstId.isBlank() || secondId.isBlank() || firstId == secondId) { finish(); return }
        viewModel.onEvent(CompareResultUiEvent.Opened(
            firstId, secondId, intent.getBooleanExtra(EXTRA_PREVIEW_ERROR, false)
        ))
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch { viewModel.uiState.collect(::render) }
            launch { viewModel.uiEffect.collect { if (it == CompareResultUiEffect.NavigateBack) finish() } }
        } }
    }

    private fun render(state: CompareResultUiState) = with(binding) {
        compareResultDocumentA.text = getString(R.string.compare_document_a, state.documentA)
        compareResultDocumentB.text = getString(R.string.compare_document_b, state.documentB)
        compareResultLoading.isVisible = state.status == CompareResultStatus.LOADING
        compareResultError.isVisible = state.status == CompareResultStatus.ERROR
        compareResultNoExample.isVisible = state.status == CompareResultStatus.NO_EXAMPLE
        val ready = state.status == CompareResultStatus.READY
        compareOverviewContent.isVisible = ready && state.selectedTab == CompareResultTab.OVERVIEW
        compareRiskContent.isVisible = ready && state.selectedTab == CompareResultTab.RISK
        compareClausesContent.isVisible = ready && state.selectedTab == CompareResultTab.CLAUSES
        if (ready && visibleTab != null && visibleTab != state.selectedTab) {
            when (state.selectedTab) {
                CompareResultTab.OVERVIEW -> compareOverviewContent
                CompareResultTab.RISK -> compareRiskContent
                CompareResultTab.CLAUSES -> compareClausesContent
            }.animateContentEntrance(0f)
        }
        visibleTab = if (ready) state.selectedTab else null
        state.comparison?.let { comparison ->
            val rows = listOf(compareOverviewType, compareOverviewTerm, compareOverviewFee, compareOverviewTrial, compareOverviewPayment, compareOverviewDamages)
            rows.forEachIndexed { index, row ->
                val difference = comparison.differences.getOrNull(index)
                row.root.isVisible = difference != null
                if (difference != null) {
                    row.compareOverviewTitle.text = difference.section
                    row.compareOverviewA.text = difference.firstText.ifBlank { "—" }
                    row.compareOverviewB.text = difference.secondText.ifBlank { "—" }
                }
            }
            val differences = comparison.differences
            val sections = listOf("payment", "term", "trial", "cycle")
            val differencesByIndex = (0..3).map(differences::getOrNull)
            val firstDifference = differencesByIndex[0]
            comparePaymentDetails.isVisible = firstDifference != null && sections[0] in state.expandedSections
            firstDifference?.let { difference ->
                (comparePaymentDetails.getChildAt(0) as? TextView)?.text = "A · ${difference.firstText.ifBlank { "—" }}"
                (comparePaymentDetails.getChildAt(1) as? TextView)?.text = "B · ${difference.secondText.ifBlank { "—" }}"
                (comparePaymentDetails.getChildAt(2) as? TextView)?.text = getString(R.string.compare_difference_demo_explanation)
            }
            comparePaymentToggle.text = firstDifference?.section ?: getString(R.string.compare_no_text_difference)
            comparePaymentToggle.isEnabled = firstDifference != null
            listOf(compareTermDetails, compareTrialDetails, compareCycleDetails).forEachIndexed { index, view ->
                val difference = differencesByIndex[index + 1]
                val sectionKey = sections[index + 1]
                view.isVisible = difference != null && sectionKey in state.expandedSections
                if (difference != null) view.text = "A · ${difference.firstText.ifBlank { "—" }}\n\nB · ${difference.secondText.ifBlank { "—" }}"
                val title = difference?.section ?: getString(R.string.compare_no_text_difference)
                val toggle = listOf(compareTermToggle, compareTrialToggle, compareCycleToggle)[index]
                (toggle.getChildAt(0) as? TextView)?.text = title
                toggle.isEnabled = difference != null
            }
            (compareClausesContent.getChildAt(0) as? TextView)?.text = getString(R.string.compare_result_demo_notice)
            compareRiskContent.getChildAt(0)?.isVisible = false
            (compareRiskContent.getChildAt(1) as? TextView)?.text = getString(R.string.compare_result_demo_heading)
            for (index in 0..2) {
                val textView = compareRiskContent.getChildAt(index + 2) as? TextView ?: continue
                val difference = differences.getOrNull(index)
                textView.isVisible = difference != null
                if (difference != null) textView.text = "${difference.section}\nA: ${difference.firstText.ifBlank { "—" }}\nB: ${difference.secondText.ifBlank { "—" }}"
            }
            compareResultNotice.text = getString(R.string.compare_result_demo_notice)
        }
        comparePaymentDetails.isVisible = "payment" in state.expandedSections
        compareTermDetails.isVisible = "term" in state.expandedSections
        compareTrialDetails.isVisible = "trial" in state.expandedSections
        compareCycleDetails.isVisible = "cycle" in state.expandedSections
        listOf(compareTabOverview to CompareResultTab.OVERVIEW, compareTabRisk to CompareResultTab.RISK, compareTabClauses to CompareResultTab.CLAUSES).forEach { (view, tab) ->
            updateTab(view, state.selectedTab == tab, ready)
        }
    }

    private fun updateTab(view: TextView, selected: Boolean, enabled: Boolean) {
        view.isEnabled = enabled
        view.setBackgroundResource(if (selected) R.drawable.bg_contract_search_result else android.R.color.transparent)
        view.setTextColor(getColor(if (selected) R.color.legal_lens_primary else R.color.legal_lens_secondary_text))
    }

    companion object {
        private const val EXTRA_FIRST_ID = "compare_first_id"
        private const val EXTRA_SECOND_ID = "compare_second_id"
        private const val EXTRA_FIRST_NAME = "compare_first_name"
        private const val EXTRA_SECOND_NAME = "compare_second_name"
        private const val EXTRA_PREVIEW_ERROR = "compare_preview_error"
        fun createIntent(context: Context, first: CompareContract, second: CompareContract, previewError: Boolean = false): Intent {
            return Intent(context, CompareResultActivity::class.java).putExtra(EXTRA_FIRST_ID, first.id).putExtra(EXTRA_SECOND_ID, second.id).putExtra(EXTRA_FIRST_NAME, first.title).putExtra(EXTRA_SECOND_NAME, second.title).putExtra(EXTRA_PREVIEW_ERROR, previewError)
        }
    }
}
