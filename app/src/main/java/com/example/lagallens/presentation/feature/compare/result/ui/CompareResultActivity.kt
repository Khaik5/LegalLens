package com.example.lagallens.presentation.feature.compare.result.ui

import android.content.Context
import com.example.lagallens.presentation.feature.compare.result.utils.toClauseHeading
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityCompareResultBinding
import com.example.lagallens.core.utils.toReadableText
import com.example.lagallens.databinding.ItemCompareClauseBinding
import com.example.lagallens.databinding.ItemCompareOverviewBinding
import com.example.lagallens.domain.model.ContractChangeType
import com.example.lagallens.domain.model.ContractFieldDifference
import com.example.lagallens.domain.model.ContractFieldValue
import com.example.lagallens.domain.model.ContractSectionKind
import com.example.lagallens.presentation.feature.compare.result.utils.pairKeyFacts
import kotlin.math.roundToInt
import com.example.lagallens.domain.model.EmploymentContractComparison
import com.example.lagallens.domain.model.EmploymentContractRisk
import com.example.lagallens.presentation.common.utils.applySystemBarInsets
import com.example.lagallens.presentation.common.utils.animateContentEntrance
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import com.example.lagallens.presentation.feature.compare.result.contract.CompareResultStatus
import com.example.lagallens.presentation.feature.compare.result.contract.CompareResultTab
import com.example.lagallens.presentation.feature.compare.result.contract.CompareResultUiEffect
import com.example.lagallens.presentation.feature.compare.result.contract.CompareResultUiEvent
import com.example.lagallens.presentation.feature.compare.result.contract.CompareResultUiState
import com.example.lagallens.presentation.feature.compare.result.utils.CompareFieldFormatter
import com.example.lagallens.presentation.feature.compare.result.viewmodel.CompareResultViewModel
import com.example.lagallens.presentation.feature.compare.select.contract.CompareContract
import kotlinx.coroutines.launch
import java.util.Locale

class CompareResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCompareResultBinding
    private val viewModel by viewModels<CompareResultViewModel>()
    private var visibleTab: CompareResultTab? = null
    private var renderedComparison: EmploymentContractComparison? = null
    private val fieldFormatter by lazy { CompareFieldFormatter(this) }

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
            binding.compareTabRisk, binding.compareTabClauses).forEach { it.bindPressFeedback() }
        binding.compareResultBack.setOnClickListener { viewModel.onEvent(CompareResultUiEvent.BackClicked) }
        binding.compareResultRetry.setOnClickListener { viewModel.onEvent(CompareResultUiEvent.RetryClicked) }
        binding.compareTabOverview.setOnClickListener { viewModel.onEvent(CompareResultUiEvent.TabClicked(CompareResultTab.OVERVIEW)) }
        binding.compareTabRisk.setOnClickListener { viewModel.onEvent(CompareResultUiEvent.TabClicked(CompareResultTab.RISK)) }
        binding.compareTabClauses.setOnClickListener { viewModel.onEvent(CompareResultUiEvent.TabClicked(CompareResultTab.CLAUSES)) }
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
        compareResultErrorMessage.setText(state.errorMessageRes ?: R.string.compare_error_message)
        compareResultNoExample.isVisible = state.status == CompareResultStatus.NO_EXAMPLE
        val ready = state.status == CompareResultStatus.READY
        // Without a finished comparison there is nothing to switch between, so the tabs are hidden instead of looking broken.
        (compareTabOverview.parent as android.view.View).isVisible = ready
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
        val comparison = state.comparison
        if (comparison == null) {
            renderedComparison = null
        } else {
            if (comparison !== renderedComparison) {
                renderedComparison = comparison
                renderOverview(comparison)
                renderRisk(comparison)
            }
            renderClauses(comparison, state.expandedSections)
        }
        compareResultNotice.isVisible = comparison?.isSample == true
        listOf(compareTabOverview to CompareResultTab.OVERVIEW, compareTabRisk to CompareResultTab.RISK, compareTabClauses to CompareResultTab.CLAUSES).forEach { (view, tab) ->
            updateTab(view, state.selectedTab == tab, ready)
        }
    }

    private fun renderOverview(comparison: EmploymentContractComparison) = with(binding.compareOverviewContent) {
        removeAllViews()
        // The backend also lists key facts as "keyFact:<label>" fields paired by exact label, which splits the same
        // topic named differently into a removed and an added row; they are shown once below, paired by topic.
        val changedFields = comparison.fieldDifferences.filter {
            it.changeType != ContractChangeType.UNCHANGED && it.isInformative() &&
                !(it.field.startsWith(KEY_FACT_FIELD_PREFIX) && (comparison.firstKeyFacts.isNotEmpty() || comparison.secondKeyFacts.isNotEmpty()))
        }
        // Backend comparisons list labelled fields; only DEMO ones fall back to their text sections.
        val rows = when {
            changedFields.isNotEmpty() -> changedFields.map { difference ->
                Triple(
                    fieldFormatter.label(difference.field, difference.label),
                    difference.firstValue?.let { fieldFormatter.value(difference.field, it) },
                    difference.secondValue?.let { fieldFormatter.value(difference.field, it) }
                )
            }
            comparison.isSample -> comparison.differences.filter { it.changeType != ContractChangeType.UNCHANGED }
                .take(OVERVIEW_SAMPLE_ROWS).map { Triple(it.section, it.firstText, it.secondText) }
            else -> emptyList()
        }
        val keyFactRows = keyFactRows(comparison)
        if (rows.isEmpty() && keyFactRows.isEmpty()) {
            val message = if (comparison.fieldDifferences.any { it.isInformative() }) R.string.compare_overview_empty else R.string.compare_overview_no_data
            addView(messageView(message))
            return@with
        }
        rows.forEach { (label, first, second) -> addOverviewRow(label, first, second) }
        if (keyFactRows.isNotEmpty()) {
            addView(sectionTitleView(R.string.compare_key_facts_heading))
            keyFactRows.forEach { (label, first, second) -> addOverviewRow(label, first, second) }
            if (comparison.isKeyFactsPreview) addView(messageView(R.string.compare_key_facts_preview_note))
        }
    }

    private fun android.widget.LinearLayout.addOverviewRow(label: String, first: String?, second: String?) {
        val row = ItemCompareOverviewBinding.inflate(LayoutInflater.from(context), this, false)
        row.compareOverviewTitle.text = label
        row.compareOverviewA.text = first?.takeIf { it.isNotBlank() } ?: getString(R.string.compare_value_missing)
        row.compareOverviewB.text = second?.takeIf { it.isNotBlank() } ?: getString(R.string.compare_value_missing)
        addView(row.root)
    }

    private fun keyFactRows(comparison: EmploymentContractComparison): List<Triple<String, String?, String?>> =
        pairKeyFacts(comparison.firstKeyFacts, comparison.secondKeyFacts).map { Triple(it.label, it.first, it.second) }

    private fun sectionTitleView(@StringRes text: Int) = TextView(this).apply {
        layoutParams = android.widget.LinearLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = resources.getDimensionPixelSize(R.dimen.dp_20) }
        setText(text)
        setTextColor(ContextCompat.getColor(context, R.color.legal_lens_primary))
        setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.sp_15))
        setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun renderRisk(comparison: EmploymentContractComparison) = with(binding) {
        val risk = comparison.riskComparison
        // The note explains a missing comparison, or warns that a score belongs to another version of the document.
        val staleSource = risk != null && (risk.first.matchesSource == false || risk.second.matchesSource == false)
        compareRiskEmpty.isVisible = risk == null || staleSource
        compareRiskEmpty.setText(if (risk == null) R.string.compare_risk_not_analyzed else R.string.compare_risk_source_mismatch)
        compareRiskScores.isVisible = risk != null
        compareRiskDelta.isVisible = risk?.scoreDelta != null
        if (risk == null) return@with
        bindScore(compareRiskScoreA, compareRiskLevelA, risk.first.score, risk.first.risk)
        bindScore(compareRiskScoreB, compareRiskLevelB, risk.second.score, risk.second.risk)
        risk.scoreDelta?.let { delta ->
            compareRiskDelta.text = getString(R.string.compare_risk_delta, String.format(Locale.getDefault(), "%+.1f", delta))
        }
    }

    private fun bindScore(scoreView: TextView, levelView: TextView, score: Double?, risk: EmploymentContractRisk) {
        scoreView.text = score?.let { getString(R.string.compare_risk_score_value, String.format(Locale.getDefault(), "%.0f", it)) }
            ?: getString(R.string.compare_value_missing)
        scoreView.setTextColor(ContextCompat.getColor(this, riskColor(risk)))
        levelView.setText(riskLabel(risk))
    }

    private fun renderClauses(comparison: EmploymentContractComparison, expanded: Set<String>) = with(binding) {
        val changed = comparison.differences.filter { it.changeType != ContractChangeType.UNCHANGED }
        val summary = comparison.summary
        compareClausesSummary.isVisible = !comparison.isSample
        compareClausesHint.isVisible = !comparison.isSample && changed.isNotEmpty()
        compareClausesSummary.text = getString(
            R.string.compare_clauses_summary, summary.changedSections, summary.addedSections, summary.removedSections
        )
        compareClausesEmpty.isVisible = changed.isEmpty()
        compareClausesList.removeAllViews()
        changed.forEachIndexed { index, difference ->
            val key = "clause:$index"
            val item = ItemCompareClauseBinding.inflate(LayoutInflater.from(this@CompareResultActivity), compareClausesList, false)
            item.compareClauseTitle.text =
                if (difference.kind == ContractSectionKind.PREAMBLE) getString(R.string.compare_section_preamble) else difference.section.toReadableText().toClauseHeading()
            item.compareClauseChange.text = changeText(difference)
            item.compareClauseChange.setTextColor(ContextCompat.getColor(this@CompareResultActivity, changeColor(difference.changeType)))
            item.compareClauseDetails.isVisible = key in expanded
            item.compareClauseTextA.text = getString(R.string.compare_clause_text_a, difference.firstText.toReadableText().ifBlank { getString(R.string.compare_value_missing) })
            item.compareClauseTextB.text = getString(R.string.compare_clause_text_b, difference.secondText.toReadableText().ifBlank { getString(R.string.compare_value_missing) })
            item.compareClauseHeader.bindPressFeedback()
            item.compareClauseHeader.setOnClickListener { viewModel.onEvent(CompareResultUiEvent.SectionClicked(key)) }
            compareClausesList.addView(item.root)
        }
    }

    /** The backend reports contract type KHAC while it does not know the type, so two of them say nothing about the contracts. */
    private fun ContractFieldDifference.isInformative() =
        !(field == "contractType" && changeType == ContractChangeType.UNCHANGED && firstValue == ContractFieldValue.Text("KHAC"))

    private fun messageView(@StringRes text: Int) = TextView(this).apply {
        layoutParams = android.widget.LinearLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = resources.getDimensionPixelSize(R.dimen.dp_16) }
        setText(text)
        setTextColor(ContextCompat.getColor(context, R.color.legal_lens_secondary_text))
        val padding = resources.getDimensionPixelSize(R.dimen.dp_16)
        setPadding(padding, padding, padding, padding)
        setBackgroundResource(R.drawable.bg_compare_notice)
    }

    /** The change, plus how alike both sides are and whether the clause was renumbered, when the backend says so. */
    private fun changeText(difference: com.example.lagallens.domain.model.ContractTextDifference): String {
        val parts = mutableListOf(getString(changeLabel(difference.changeType)))
        if (difference.changeType == ContractChangeType.MODIFIED) {
            difference.similarity?.let { parts += getString(R.string.compare_change_similarity, (it * 100).roundToInt()) }
        }
        if (difference.isRenumbered) parts += getString(R.string.compare_change_renumbered)
        return parts.joinToString(" · ")
    }

    @StringRes private fun changeLabel(type: ContractChangeType) = when (type) {
        ContractChangeType.ADDED -> R.string.compare_change_added
        ContractChangeType.REMOVED -> R.string.compare_change_removed
        else -> R.string.compare_change_modified
    }

    @ColorRes private fun changeColor(type: ContractChangeType) = when (type) {
        ContractChangeType.ADDED -> R.color.contract_complete
        ContractChangeType.REMOVED -> R.color.contract_risk_high
        else -> R.color.contract_pending
    }

    @StringRes private fun riskLabel(risk: EmploymentContractRisk) = when (risk) {
        EmploymentContractRisk.LOW -> R.string.contract_low_risk
        EmploymentContractRisk.MEDIUM -> R.string.contract_medium_risk
        EmploymentContractRisk.HIGH, EmploymentContractRisk.CRITICAL -> R.string.contract_high_risk
        EmploymentContractRisk.NOT_ANALYZED -> R.string.compare_risk_not_analyzed_short
    }

    @ColorRes private fun riskColor(risk: EmploymentContractRisk) = when (risk) {
        EmploymentContractRisk.LOW -> R.color.contract_complete
        EmploymentContractRisk.MEDIUM -> R.color.contract_pending
        EmploymentContractRisk.HIGH, EmploymentContractRisk.CRITICAL -> R.color.contract_risk_high
        EmploymentContractRisk.NOT_ANALYZED -> R.color.legal_lens_secondary_text
    }

    private fun updateTab(view: TextView, selected: Boolean, enabled: Boolean) {
        view.isEnabled = enabled
        view.setBackgroundResource(if (selected) R.drawable.bg_contract_search_result else android.R.color.transparent)
        view.setTextColor(getColor(if (selected) R.color.legal_lens_primary else R.color.legal_lens_secondary_text))
    }

    companion object {
        private const val OVERVIEW_SAMPLE_ROWS = 6
        private const val KEY_FACT_FIELD_PREFIX = "keyFact:"
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
