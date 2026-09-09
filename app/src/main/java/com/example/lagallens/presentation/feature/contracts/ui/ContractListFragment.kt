package com.example.lagallens.presentation.feature.contracts.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lagallens.R
import com.example.lagallens.databinding.FragmentContractListBinding
import com.example.lagallens.presentation.feature.contracts.adapter.ContractListAdapter
import com.example.lagallens.presentation.feature.contracts.contract.ContractFilter
import com.example.lagallens.presentation.feature.contracts.contract.ContractListUiEffect
import com.example.lagallens.presentation.feature.contracts.contract.ContractListUiEvent
import com.example.lagallens.presentation.feature.contracts.contract.ContractListUiState
import com.example.lagallens.presentation.feature.contracts.filter.ui.ContractFilterBottomSheet
import com.example.lagallens.presentation.feature.contracts.detail.ui.ContractDetailFragment
import com.example.lagallens.presentation.feature.contracts.viewmodel.ContractListViewModel
import kotlinx.coroutines.launch

class ContractListFragment : Fragment() {
    private var _binding: FragmentContractListBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModels<ContractListViewModel>()
    private val contractAdapter = ContractListAdapter { contract ->
        viewModel.onEvent(ContractListUiEvent.ContractClicked(contract))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContractListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindView()
        bindFilterResult()
        collectUiState()
        collectUiEffect()
    }

    private fun bindView() {
        binding.rvContracts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = contractAdapter
        }
        binding.etSearch.doAfterTextChanged {
            viewModel.onEvent(ContractListUiEvent.QueryChanged(it?.toString().orEmpty()))
        }
        binding.btnSearch.setOnClickListener {
            val navController = findNavController()
            if (navController.currentDestination?.id == R.id.contractListFragment) {
                navController.navigate(R.id.action_contractListFragment_to_contractSearchFragment)
            }
        }
        binding.btnFilter.setOnClickListener {
            viewModel.onEvent(ContractListUiEvent.FilterClicked)
        }
        binding.tvFilterAll.setOnClickListener { selectFilter(ContractFilter.ALL) }
        binding.tvFilterAnalyzed.setOnClickListener { selectFilter(ContractFilter.ANALYZED) }
        binding.tvFilterProcessing.setOnClickListener { selectFilter(ContractFilter.PROCESSING) }
        binding.tvFilterHighRisk.setOnClickListener { selectFilter(ContractFilter.HIGH_RISK) }
    }

    private fun bindFilterResult() {
        parentFragmentManager.setFragmentResultListener(
            ContractFilterBottomSheet.RESULT_KEY,
            viewLifecycleOwner
        ) { _, result ->
            viewModel.onEvent(
                ContractListUiEvent.FiltersApplied(
                    ContractFilterBottomSheet.readSelection(result)
                )
            )
        }
    }

    private fun selectFilter(filter: ContractFilter) {
        viewModel.onEvent(ContractListUiEvent.FilterSelected(filter))
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
                        ContractListUiEffect.ShowFilterBottomSheet -> {
                            ContractFilterBottomSheet().show(
                                parentFragmentManager,
                                ContractFilterBottomSheet.TAG
                            )
                        }
                        is ContractListUiEffect.NavigateToDetail -> {
                            val navController = findNavController()
                            if (navController.currentDestination?.id == R.id.contractListFragment) {
                                navController.navigate(
                                    R.id.action_contractListFragment_to_contractDetailFragment,
                                    bundleOf(ContractDetailFragment.ARG_CONTRACT_ID to effect.contractId)
                                )
                            }
                        }
                        is ContractListUiEffect.ShowMessage -> {
                            Toast.makeText(requireContext(), effect.messageRes, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun render(state: ContractListUiState) {
        contractAdapter.submitList(state.contracts)
        updateFilterStyle(binding.tvFilterAll, state.selectedFilter == ContractFilter.ALL)
        updateFilterStyle(binding.tvFilterAnalyzed, state.selectedFilter == ContractFilter.ANALYZED)
        updateFilterStyle(binding.tvFilterProcessing, state.selectedFilter == ContractFilter.PROCESSING)
        updateFilterStyle(binding.tvFilterHighRisk, state.selectedFilter == ContractFilter.HIGH_RISK)
    }

    private fun updateFilterStyle(view: View, isSelected: Boolean) {
        view.setBackgroundResource(
            if (isSelected) R.drawable.bg_contract_filter_active else R.drawable.bg_contract_filter_inactive
        )
        (view as android.widget.TextView).setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (isSelected) R.color.white else R.color.legal_lens_secondary_text
            )
        )
    }

    override fun onDestroyView() {
        binding.rvContracts.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
