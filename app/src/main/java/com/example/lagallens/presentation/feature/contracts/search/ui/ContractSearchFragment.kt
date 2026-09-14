package com.example.lagallens.presentation.feature.contracts.search.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
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
import com.example.lagallens.databinding.FragmentContractSearchBinding
import com.example.lagallens.presentation.feature.contracts.search.adapter.ContractSearchAdapter
import com.example.lagallens.presentation.feature.contracts.detail.ui.ContractDetailFragment
import com.example.lagallens.presentation.feature.contracts.search.contract.ContractSearchUiEffect
import com.example.lagallens.presentation.feature.contracts.search.contract.ContractSearchUiEvent
import com.example.lagallens.presentation.feature.contracts.search.contract.ContractSearchUiState
import com.example.lagallens.presentation.feature.contracts.search.viewmodel.ContractSearchViewModel
import kotlinx.coroutines.launch

class ContractSearchFragment : Fragment() {
    private var _binding: FragmentContractSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModels<ContractSearchViewModel>()
    private val searchAdapter = ContractSearchAdapter { result ->
        viewModel.onEvent(ContractSearchUiEvent.ResultClicked(result))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContractSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindView()
        collectUiState()
        collectUiEffect()
    }

    private fun bindView() {
        binding.rvResults.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchAdapter
        }
        binding.btnBack.setOnClickListener {
            viewModel.onEvent(ContractSearchUiEvent.BackClicked)
        }
        binding.btnClear.setOnClickListener {
            viewModel.onEvent(ContractSearchUiEvent.ClearClicked)
        }
        binding.etSearch.doAfterTextChanged {
            viewModel.onEvent(ContractSearchUiEvent.QueryChanged(it?.toString().orEmpty()))
        }
        binding.tvRecentBitexco.setOnClickListener {
            selectRecentSearch(R.string.contract_search_recent_bitexco)
        }
        binding.tvRecentNda.setOnClickListener {
            selectRecentSearch(R.string.contract_search_recent_nda)
        }
        binding.tvRecentLabor.setOnClickListener {
            selectRecentSearch(R.string.contract_search_recent_labor)
        }
        binding.tvRecentEquipment.setOnClickListener {
            selectRecentSearch(R.string.contract_search_recent_equipment)
        }
    }

    private fun selectRecentSearch(queryRes: Int) {
        viewModel.onEvent(ContractSearchUiEvent.RecentSearchClicked(getString(queryRes)))
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
                        ContractSearchUiEffect.NavigateBack -> {
                            val navController = findNavController()
                            if (navController.currentDestination?.id == R.id.contractSearchFragment) {
                                navController.navigateUp()
                            }
                        }
                        is ContractSearchUiEffect.NavigateToDetail -> {
                            val navController = findNavController()
                            if (navController.currentDestination?.id == R.id.contractSearchFragment) {
                                navController.navigate(
                                    R.id.action_contractSearchFragment_to_contractDetailFragment,
                                    bundleOf(ContractDetailFragment.ARG_CONTRACT_ID to effect.contractId)
                                )
                            }
                        }
                        is ContractSearchUiEffect.ShowMessage -> {
                            Toast.makeText(requireContext(), effect.messageRes, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun render(state: ContractSearchUiState) {
        if (binding.etSearch.text.toString() != state.query) {
            binding.etSearch.setText(state.query)
            binding.etSearch.setSelection(state.query.length)
        }
        binding.btnClear.isVisible = state.query.isNotBlank()
        binding.tvResultsTitle.text = getString(R.string.contract_search_results, state.results.size)
        searchAdapter.submitList(state.results)
    }

    override fun onDestroyView() {
        binding.rvResults.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
