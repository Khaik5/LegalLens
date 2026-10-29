package com.example.lagallens.presentation.feature.contracts.detail.export.ui

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.R
import com.example.lagallens.databinding.BottomSheetContractExportBinding
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import com.example.lagallens.presentation.feature.contracts.detail.export.contract.ContractExportStatus
import com.example.lagallens.presentation.feature.contracts.detail.export.contract.ContractExportUiEffect
import com.example.lagallens.presentation.feature.contracts.detail.export.contract.ContractExportUiEvent
import com.example.lagallens.presentation.feature.contracts.detail.export.contract.ContractExportUiState
import com.example.lagallens.presentation.feature.contracts.detail.export.viewmodel.ContractExportViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class ContractExportBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetContractExportBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModels<ContractExportViewModel>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = BottomSheetDialog(requireContext(), theme).apply {
        setOnShowListener {
            val sheet = findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet) ?: return@setOnShowListener
            sheet.setBackgroundColor(Color.TRANSPARENT)
            BottomSheetBehavior.from(sheet).state = BottomSheetBehavior.STATE_EXPANDED
        }
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetContractExportBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listOf(binding.exportSave, binding.exportShare, binding.exportRetry, binding.exportCancel).forEach { it.bindPressFeedback() }
        binding.exportSave.setOnClickListener { viewModel.onEvent(ContractExportUiEvent.ExportClicked) }
        binding.exportShare.setOnClickListener { viewModel.onEvent(ContractExportUiEvent.ShareClicked) }
        binding.exportRetry.setOnClickListener { viewModel.onEvent(ContractExportUiEvent.RetryClicked) }
        binding.exportCancel.setOnClickListener { viewModel.onEvent(ContractExportUiEvent.CancelClicked) }
        viewModel.onEvent(ContractExportUiEvent.Opened(arguments?.getString(KEY_TITLE).orEmpty(), arguments?.getString(KEY_METADATA).orEmpty(), arguments?.getBoolean(KEY_READY_PREVIEW) == true))
        viewLifecycleOwner.lifecycleScope.launch { viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch { viewModel.uiState.collect(::render) }
            launch { viewModel.uiEffect.collect { if (it == ContractExportUiEffect.Close) dismiss() } }
        } }
    }

    private fun render(state: ContractExportUiState) = with(binding) {
        exportDocumentTitle.text = state.title
        exportDocumentMetadata.text = state.metadata
        exportDescription.isVisible = state.status == ContractExportStatus.OPTIONS
        exportDemoNotice.isVisible = state.status == ContractExportStatus.OPTIONS
        exportStatus.isVisible = state.status != ContractExportStatus.OPTIONS
        exportProgress.isVisible = state.status == ContractExportStatus.LOADING
        exportStatusIcon.isVisible = state.status == ContractExportStatus.READY_PREVIEW || state.status == ContractExportStatus.ERROR
        exportSave.isEnabled = state.status == ContractExportStatus.OPTIONS
        exportShare.isEnabled = state.status == ContractExportStatus.OPTIONS
        exportShare.alpha = if (exportShare.isEnabled) 1f else 0.5f
        exportRetry.isVisible = state.status == ContractExportStatus.ERROR
        when (state.status) {
            ContractExportStatus.LOADING -> { exportStatus.setBackgroundResource(R.drawable.bg_compare_notice); exportStatusTitle.setText(R.string.contract_export_loading); exportStatusDetail.setText(R.string.contract_export_loading_detail) }
            ContractExportStatus.READY_PREVIEW -> { exportStatus.setBackgroundResource(R.drawable.bg_contract_export_success); exportStatusIcon.setImageResource(R.drawable.ic_credit_success_check); exportStatusTitle.setText(R.string.contract_export_ready); exportStatusDetail.setText(R.string.contract_export_ready_detail) }
            ContractExportStatus.ERROR -> { exportStatus.setBackgroundResource(R.drawable.bg_contract_export_error); exportStatusIcon.setImageResource(R.drawable.ic_chatbot_error); exportStatusTitle.setText(R.string.contract_export_error); exportStatusDetail.setText(R.string.contract_export_error_detail) }
            ContractExportStatus.OPTIONS -> Unit
        }
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
    companion object {
        const val TAG = "ContractExportBottomSheet"
        private const val KEY_TITLE = "export_title"
        private const val KEY_METADATA = "export_metadata"
        private const val KEY_READY_PREVIEW = "export_ready_preview"
        fun newInstance(title: String, metadata: String, showReadyPreview: Boolean = false) = ContractExportBottomSheet().apply {
            arguments = bundleOf(KEY_TITLE to title, KEY_METADATA to metadata, KEY_READY_PREVIEW to showReadyPreview)
        }
    }
}
