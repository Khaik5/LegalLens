package com.example.lagallens.presentation.feature.chatbot.draft.save

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.R
import com.example.lagallens.databinding.BottomSheetSaveDraftCopyBinding
import com.example.lagallens.presentation.feature.chatbot.draft.save.detail.SavedDraftCopyDetailActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class SaveDraftCopyBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetSaveDraftCopyBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModels<SaveDraftCopyViewModel>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = BottomSheetDialog(requireContext(), theme).apply {
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setOnShowListener {
            val sheet = findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet) ?: return@setOnShowListener
            sheet.setBackgroundColor(Color.TRANSPARENT)
            BottomSheetBehavior.from(sheet).state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetSaveDraftCopyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val version = arguments?.getString(KEY_VERSION).orEmpty().ifBlank { "D2" }
        viewModel.setDraft(
            version,
            arguments?.getString(KEY_CONTENT).orEmpty(),
            arguments?.getString(KEY_CONTRACT_ID),
            arguments?.getString(KEY_SOURCE_DRAFT_ID)
        )
        binding.saveDraftName.setText(viewModel.uiState.value.name)
        binding.saveDraftAcknowledge.isChecked = viewModel.uiState.value.acknowledged
        binding.saveDraftName.doAfterTextChangedCompat { viewModel.setName(it) }
        binding.saveDraftAcknowledge.setOnCheckedChangeListener { _, checked -> viewModel.setAcknowledged(checked) }
        binding.saveDraftConfirm.setOnClickListener { viewModel.save() }
        binding.saveDraftRetry.setOnClickListener { viewModel.retry() }
        binding.saveDraftCancel.setOnClickListener { dismiss() }
        binding.saveDraftReturn.setOnClickListener { dismiss() }
        binding.saveDraftViewSaved.setOnClickListener {
            startActivity(SavedDraftCopyDetailActivity.createIntent(requireContext(), version))
            dismiss()
        }
        binding.saveDraftExportPdf.setOnClickListener {
            startActivity(SavedDraftCopyDetailActivity.createIntent(requireContext(), version, openExport = true))
            dismiss()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.uiState.collect(::render) }
        }
    }

    private fun render(state: SaveDraftCopyUiState) = with(binding) {
        saveDraftName.setTextIfDifferent(state.name)
        saveDraftAcknowledge.isChecked = state.acknowledged
        val form = state.status == SaveDraftCopyStatus.FORM
        saveDraftForm.isVisible = form
        saveDraftLoading.isVisible = state.status == SaveDraftCopyStatus.LOADING
        saveDraftError.isVisible = state.status == SaveDraftCopyStatus.ERROR
        saveDraftSuccess.isVisible = state.status == SaveDraftCopyStatus.SUCCESS
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }

    private fun android.widget.EditText.doAfterTextChangedCompat(onChanged: (String) -> Unit) {
        addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = onChanged(s?.toString().orEmpty())
            override fun afterTextChanged(s: android.text.Editable?) = Unit
        })
    }

    private fun android.widget.EditText.setTextIfDifferent(value: String) {
        if (text?.toString() != value) setText(value)
    }

    companion object {
        const val TAG = "SaveDraftCopyBottomSheet"
        private const val KEY_VERSION = "save_draft_version"
        private const val KEY_CONTENT = "save_draft_content"
        private const val KEY_CONTRACT_ID = "save_draft_contract_id"
        private const val KEY_SOURCE_DRAFT_ID = "save_draft_source_draft_id"
        fun newInstance(version: String = "D2", content: String = "", contractId: String? = null, sourceDraftId: String? = null) = SaveDraftCopyBottomSheet().apply {
            arguments = android.os.Bundle().apply {
                putString(KEY_VERSION, version)
                putString(KEY_CONTENT, content)
                putString(KEY_CONTRACT_ID, contractId)
                putString(KEY_SOURCE_DRAFT_ID, sourceDraftId)
            }
        }
    }
}
