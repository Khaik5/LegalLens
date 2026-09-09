package com.example.lagallens.presentation.feature.contracts.detail.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lagallens.R
import com.example.lagallens.databinding.FragmentContractDetailBinding
import com.example.lagallens.presentation.feature.contracts.detail.adapter.ContractAssistantAdapter
import com.example.lagallens.presentation.feature.contracts.detail.contract.ContractDetailUiEffect
import com.example.lagallens.presentation.feature.contracts.detail.contract.ContractDetailUiEvent
import com.example.lagallens.presentation.feature.contracts.detail.contract.ContractDetailUiState
import com.example.lagallens.presentation.feature.contracts.detail.viewmodel.ContractDetailViewModel
import com.example.lagallens.presentation.feature.contracts.detail.dialog.ContractDeleteDialog
import kotlinx.coroutines.launch

class ContractDetailFragment : Fragment() {
    private var _binding: FragmentContractDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModels<ContractDetailViewModel>()
    private val assistantAdapter = ContractAssistantAdapter { action ->
        viewModel.onEvent(ContractDetailUiEvent.AssistantActionClicked(action))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentContractDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.loadContract(arguments?.getString(ARG_CONTRACT_ID))
        binding.rvAssistantActions.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = assistantAdapter
        }
        binding.btnBack.setOnClickListener { viewModel.onEvent(ContractDetailUiEvent.BackClicked) }
        binding.btnRename.setOnClickListener { viewModel.onEvent(ContractDetailUiEvent.RenameClicked) }
        binding.btnDelete.setOnClickListener { viewModel.onEvent(ContractDetailUiEvent.DeleteClicked) }
        parentFragmentManager.setFragmentResultListener(
            ContractDeleteDialog.RESULT_KEY,
            viewLifecycleOwner
        ) { _, result ->
            if (result.getBoolean(ContractDeleteDialog.KEY_DELETE_CONFIRMED)) {
                viewModel.onEvent(ContractDetailUiEvent.DeleteConfirmed)
            }
        }
        collectUiState()
        collectUiEffect()
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
                        ContractDetailUiEffect.NavigateBack -> findNavController().navigateUp()
                        ContractDetailUiEffect.ShowDeleteConfirmation -> showDeleteConfirmation()
                        is ContractDetailUiEffect.ShowMessage -> Toast.makeText(requireContext(), effect.messageRes, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun render(state: ContractDetailUiState) {
        binding.tvContractTitle.setText(state.titleRes)
        binding.tvContractType.setText(state.contractTypeRes)
        binding.tvStatus.setText(state.statusRes)
        binding.tvRisk.setText(state.riskRes)
        binding.tvCreatedAt.setText(state.createdAtRes)
        binding.tvUpdatedAt.setText(state.updatedAtRes)
        binding.tvPageCount.setText(state.pageCountRes)
        binding.tvFileFormat.setText(state.fileFormatRes)
        binding.tvStatus.setBackgroundResource(state.statusBackgroundRes)
        binding.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), state.statusTextColorRes))
        binding.tvRisk.setBackgroundResource(state.riskBackgroundRes)
        binding.tvRisk.setTextColor(ContextCompat.getColor(requireContext(), state.riskTextColorRes))
        assistantAdapter.submitList(state.assistantActions)
    }

    private fun showDeleteConfirmation() {
        if (parentFragmentManager.findFragmentByTag(ContractDeleteDialog.TAG) == null) {
            ContractDeleteDialog().show(parentFragmentManager, ContractDeleteDialog.TAG)
        }
    }

    override fun onDestroyView() {
        binding.rvAssistantActions.adapter = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_CONTRACT_ID = "contractId"
    }
}
