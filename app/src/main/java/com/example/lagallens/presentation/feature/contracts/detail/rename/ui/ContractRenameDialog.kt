package com.example.lagallens.presentation.feature.contracts.detail.rename.ui

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.R
import com.example.lagallens.databinding.DialogContractRenameBinding
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import com.example.lagallens.presentation.feature.contracts.detail.rename.contract.ContractRenameUiEffect
import com.example.lagallens.presentation.feature.contracts.detail.rename.contract.ContractRenameUiEvent
import com.example.lagallens.presentation.feature.contracts.detail.rename.contract.ContractRenameUiState
import com.example.lagallens.presentation.feature.contracts.detail.rename.viewmodel.ContractRenameViewModel
import kotlinx.coroutines.launch

class ContractRenameDialog : DialogFragment() {
    private var _binding: DialogContractRenameBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModels<ContractRenameViewModel>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = Dialog(requireContext()).apply { requestWindowFeature(Window.FEATURE_NO_TITLE) }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogContractRenameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listOf(binding.renameCancel, binding.renameSave).forEach { it.bindPressFeedback() }
        binding.renameCard.setOnClickListener { }
        binding.renameOverlay.setOnClickListener { viewModel.onEvent(ContractRenameUiEvent.CancelClicked) }
        binding.renameCancel.setOnClickListener { viewModel.onEvent(ContractRenameUiEvent.CancelClicked) }
        binding.renameSave.setOnClickListener { viewModel.onEvent(ContractRenameUiEvent.SaveClicked) }
        binding.renameInput.doAfterTextChanged { viewModel.onEvent(ContractRenameUiEvent.NameChanged(it?.toString().orEmpty())) }
        viewModel.onEvent(ContractRenameUiEvent.Opened(arguments?.getString(KEY_NAME).orEmpty()))
        viewLifecycleOwner.lifecycleScope.launch { viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch { viewModel.uiState.collect(::render) }
            launch { viewModel.uiEffect.collect { effect -> when (effect) {
                ContractRenameUiEffect.Close -> dismiss()
                is ContractRenameUiEffect.Saved -> { parentFragmentManager.setFragmentResult(RESULT_KEY, bundleOf(KEY_NAME to effect.name)); dismiss() }
            } } }
        } }
    }

    private fun render(state: ContractRenameUiState) {
        if (binding.renameInput.text.toString() != state.name) {
            binding.renameInput.setText(state.name)
            binding.renameInput.setSelection(state.name.length)
        }
        binding.renameInput.isEnabled = !state.isSaving
        binding.renameCancel.isEnabled = !state.isSaving
        binding.renameCancel.alpha = if (state.isSaving) 0.5f else 1f
        binding.renameSave.isEnabled = state.canSave
        binding.renameSaveLabel.setText(if (state.isSaving) R.string.contract_rename_loading else R.string.contract_rename_save)
        binding.renameProgress.isVisible = state.isSaving
        binding.renameInput.setBackgroundResource(when { state.isSaving -> R.drawable.bg_profile_edit_email; state.hasError -> R.drawable.bg_contract_rename_input_error; else -> R.drawable.bg_contract_rename_input })
        binding.renameHelper.setText(when { state.isSaving -> R.string.contract_rename_saving; state.hasError -> R.string.contract_rename_empty; else -> R.string.contract_rename_helper })
        binding.renameHelper.setTextColor(requireContext().getColor(if (state.hasError) R.color.contract_risk_high else R.color.legal_lens_secondary_text))
        isCancelable = !state.isSaving
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply { setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE); clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND); setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)); setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) }
    }
    override fun onDestroyView() { _binding = null; super.onDestroyView() }

    companion object {
        const val TAG = "ContractRenameDialog"
        const val RESULT_KEY = "contractRenameDialog"
        const val KEY_NAME = "contractName"
        fun newInstance(name: String) = ContractRenameDialog().apply { arguments = bundleOf(KEY_NAME to name) }
    }
}
