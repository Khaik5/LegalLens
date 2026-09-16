package com.example.lagallens.presentation.feature.contracts.filter.ui

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.R
import com.example.lagallens.databinding.BottomSheetContractFilterBinding
import com.example.lagallens.presentation.feature.contracts.contract.ContractFilterSelection
import com.example.lagallens.presentation.feature.contracts.contract.ContractRiskLevel
import com.example.lagallens.presentation.feature.contracts.contract.ContractStatusFilter
import com.example.lagallens.presentation.feature.contracts.contract.ContractType
import com.example.lagallens.presentation.feature.contracts.filter.contract.ContractFilterUiEffect
import com.example.lagallens.presentation.feature.contracts.filter.contract.ContractFilterUiEvent
import com.example.lagallens.presentation.feature.contracts.filter.contract.ContractFilterUiState
import com.example.lagallens.presentation.feature.contracts.filter.viewmodel.ContractFilterViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class ContractFilterBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetContractFilterBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModels<ContractFilterViewModel>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), theme).apply {
            setOnShowListener {
                val bottomSheet = findViewById<FrameLayout>(
                    com.google.android.material.R.id.design_bottom_sheet
                ) ?: return@setOnShowListener
                bottomSheet.setBackgroundColor(Color.TRANSPARENT)
                BottomSheetBehavior.from(bottomSheet).apply {
                    skipCollapsed = true
                    state = BottomSheetBehavior.STATE_EXPANDED
                }
                window?.apply {
                    navigationBarColor = ContextCompat.getColor(
                        requireContext(),
                        R.color.white
                    )
                    WindowCompat.getInsetsController(this, decorView).isAppearanceLightNavigationBars = true
                    setDimAmount(FIGMA_DIM_AMOUNT)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetContractFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindView()
        collectUiState()
        collectUiEffect()
    }

    private fun bindView() {
        binding.rowService.setOnClickListener {
            viewModel.onEvent(ContractFilterUiEvent.ContractTypeToggled(ContractType.SERVICE))
        }
        binding.rowEmployment.setOnClickListener {
            viewModel.onEvent(ContractFilterUiEvent.ContractTypeToggled(ContractType.EMPLOYMENT))
        }
        binding.rowCommercial.setOnClickListener {
            viewModel.onEvent(ContractFilterUiEvent.ContractTypeToggled(ContractType.COMMERCIAL))
        }
        binding.tvStatusAll.setOnClickListener {
            viewModel.onEvent(ContractFilterUiEvent.StatusSelected(ContractStatusFilter.ALL))
        }
        binding.tvStatusComplete.setOnClickListener {
            viewModel.onEvent(ContractFilterUiEvent.StatusSelected(ContractStatusFilter.COMPLETE))
        }
        binding.tvStatusProcessing.setOnClickListener {
            viewModel.onEvent(ContractFilterUiEvent.StatusSelected(ContractStatusFilter.PROCESSING))
        }
        binding.tvStatusDraft.setOnClickListener {
            viewModel.onEvent(ContractFilterUiEvent.StatusSelected(ContractStatusFilter.DRAFT))
        }
        binding.tvRiskLow.setOnClickListener {
            viewModel.onEvent(ContractFilterUiEvent.RiskLevelSelected(ContractRiskLevel.LOW))
        }
        binding.tvRiskMedium.setOnClickListener {
            viewModel.onEvent(ContractFilterUiEvent.RiskLevelSelected(ContractRiskLevel.MEDIUM))
        }
        binding.tvRiskHigh.setOnClickListener {
            viewModel.onEvent(ContractFilterUiEvent.RiskLevelSelected(ContractRiskLevel.HIGH))
        }
        binding.tvRiskCritical.setOnClickListener {
            viewModel.onEvent(ContractFilterUiEvent.RiskLevelSelected(ContractRiskLevel.CRITICAL))
        }
        binding.tvReset.setOnClickListener {
            viewModel.onEvent(ContractFilterUiEvent.ResetClicked)
        }
        binding.btnApply.setOnClickListener {
            viewModel.onEvent(ContractFilterUiEvent.ApplyClicked)
        }
    }

    private fun collectUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun collectUiEffect() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEffect.collect { effect ->
                    when (effect) {
                        is ContractFilterUiEffect.ApplyFilters -> {
                            parentFragmentManager.setFragmentResult(
                                RESULT_KEY,
                                selectionToBundle(effect.filterSelection)
                            )
                            dismiss()
                        }
                    }
                }
            }
        }
    }

    private fun render(state: ContractFilterUiState) {
        binding.ivServiceCheckbox.isSelected = ContractType.SERVICE in state.selection.contractTypes
        binding.ivEmploymentCheckbox.isSelected = ContractType.EMPLOYMENT in state.selection.contractTypes
        binding.ivCommercialCheckbox.isSelected = ContractType.COMMERCIAL in state.selection.contractTypes
        updateStatusStyle(binding.tvStatusAll, state.selection.status == ContractStatusFilter.ALL)
        updateStatusStyle(binding.tvStatusComplete, state.selection.status == ContractStatusFilter.COMPLETE)
        updateStatusStyle(binding.tvStatusProcessing, state.selection.status == ContractStatusFilter.PROCESSING)
        updateStatusStyle(binding.tvStatusDraft, state.selection.status == ContractStatusFilter.DRAFT)
        updateRiskStyle(binding.tvRiskLow, state.selection.riskLevel == ContractRiskLevel.LOW)
        updateRiskStyle(binding.tvRiskMedium, state.selection.riskLevel == ContractRiskLevel.MEDIUM)
        updateRiskStyle(binding.tvRiskHigh, state.selection.riskLevel == ContractRiskLevel.HIGH)
        updateRiskStyle(binding.tvRiskCritical, state.selection.riskLevel == ContractRiskLevel.CRITICAL)
    }

    private fun updateStatusStyle(view: TextView, isSelected: Boolean) {
        view.setBackgroundResource(
            if (isSelected) R.drawable.bg_contract_filter_option_selected
            else R.drawable.bg_contract_filter_option
        )
        view.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (isSelected) R.color.legal_lens_auth_accent else R.color.legal_lens_secondary_text
            )
        )
    }

    private fun updateRiskStyle(view: TextView, isSelected: Boolean) {
        view.setBackgroundResource(
            if (isSelected) R.drawable.bg_contract_filter_risk_selected
            else R.drawable.bg_contract_filter_option
        )
        view.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (isSelected) R.color.contract_risk_high else R.color.legal_lens_secondary_text
            )
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "contract_filter_bottom_sheet"
        const val RESULT_KEY = "contract_filter_result"
        private const val TYPES_KEY = "contract_filter_types"
        private const val STATUS_KEY = "contract_filter_status"
        private const val RISK_LEVEL_KEY = "contract_filter_risk_level"
        private const val FIGMA_DIM_AMOUNT = 0.24f

        fun readSelection(bundle: Bundle): ContractFilterSelection {
            return ContractFilterSelection(
                contractTypes = bundle.getStringArrayList(TYPES_KEY)
                    .orEmpty()
                    .mapNotNull { typeName -> ContractType.entries.find { it.name == typeName } }
                    .toSet(),
                status = ContractStatusFilter.entries.find {
                    it.name == bundle.getString(STATUS_KEY)
                } ?: ContractStatusFilter.ALL,
                riskLevel = ContractRiskLevel.entries.find {
                    it.name == bundle.getString(RISK_LEVEL_KEY)
                }
            )
        }

        private fun selectionToBundle(selection: ContractFilterSelection): Bundle {
            return Bundle().apply {
                putStringArrayList(TYPES_KEY, ArrayList(selection.contractTypes.map { it.name }))
                putString(STATUS_KEY, selection.status.name)
                putString(RISK_LEVEL_KEY, selection.riskLevel?.name)
            }
        }
    }
}
