package com.example.lagallens.presentation.feature.contracts.detail.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lagallens.R
import com.example.lagallens.databinding.FragmentContractDetailBinding
import com.example.lagallens.presentation.common.utils.animateContentEntrance
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import com.example.lagallens.presentation.feature.contracts.detail.adapter.ContractAssistantAdapter
import com.example.lagallens.presentation.feature.contracts.detail.contract.ContractDetailUiEffect
import com.example.lagallens.presentation.feature.contracts.detail.contract.ContractDetailUiEvent
import com.example.lagallens.presentation.feature.contracts.detail.contract.ContractDetailUiState
import com.example.lagallens.presentation.feature.contracts.detail.viewmodel.ContractDetailViewModel
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.presentation.feature.contracts.detail.dialog.ContractDeleteDialog
import com.example.lagallens.presentation.feature.contracts.detail.rename.ui.ContractRenameDialog
import com.example.lagallens.presentation.feature.contracts.detail.export.ui.ContractExportBottomSheet
import com.example.lagallens.presentation.feature.chatbot.ui.ChatbotActivity
import com.example.lagallens.presentation.feature.documentviewer.ui.DocumentViewerActivity
import com.example.lagallens.presentation.feature.compare.select.ui.CompareSelectActivity
import com.example.lagallens.presentation.feature.ocr.result.ui.OcrResultActivity
import com.example.lagallens.presentation.feature.analysis.start.ui.AnalysisStartActivity
import kotlinx.coroutines.launch

class ContractDetailFragment : Fragment() {
    private var _binding: FragmentContractDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModels<ContractDetailViewModel> {
        val app = requireActivity().application as LegalLensApplication
        ContractDetailViewModel.Factory(app.contractRepository, app.syncContractRelatedDataUseCase)
    }
    private val assistantAdapter = ContractAssistantAdapter { action ->
        viewModel.onEvent(ContractDetailUiEvent.AssistantActionClicked(action))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentContractDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listOf(binding.btnBack, binding.btnRename, binding.btnDelete).forEach { it.bindPressFeedback() }
        if (savedInstanceState == null) {
            view.doOnPreDraw {
                val current = _binding ?: return@doOnPreDraw
                val offset = resources.getDimension(R.dimen.dp_8)
                current.detailAppBar.animateContentEntrance(-offset)
                current.detailHeader.animateContentEntrance(offset)
                current.detailInformation.animateContentEntrance(offset, startDelayMillis = 35L)
            }
        }
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
        parentFragmentManager.setFragmentResultListener(ContractRenameDialog.RESULT_KEY, viewLifecycleOwner) { _, result ->
            result.getString(ContractRenameDialog.KEY_NAME)?.let { viewModel.onEvent(ContractDetailUiEvent.RenameConfirmed(it)) }
        }
        collectUiState()
        collectUiEffect()
        viewModel.loadContract(arguments?.getString(ARG_CONTRACT_ID))
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
                        is ContractDetailUiEffect.NavigateToDocument -> startActivity(
                            effect.documentUri?.let { DocumentViewerActivity.createIntent(requireContext(), android.net.Uri.parse(it), effect.title) }
                                ?: DocumentViewerActivity.createIntent(requireContext(), effect.contractId, effect.title)
                        )
                        is ContractDetailUiEffect.NavigateToCompare -> startActivity(CompareSelectActivity.createIntent(requireContext(), effect.contractId))
                        is ContractDetailUiEffect.NavigateToOcrText -> if (effect.ownerSession != null && effect.ownerSession == (requireActivity().application as LegalLensApplication).currentSessionKey()) startActivity(
                            OcrResultActivity.createIntent(
                                context = requireContext(),
                                documentName = effect.documentName,
                                documentDetails = effect.documentDetails,
                                documentId = effect.documentId,
                                contractId = effect.contractId,
                                initialContent = effect.content
                            ).putExtra(com.example.lagallens.presentation.feature.ocr.OcrSession.EXTRA_OWNER, effect.ownerSession)
                        )
                        is ContractDetailUiEffect.NavigateToAnalysis -> openAnalysis(effect)
                        is ContractDetailUiEffect.NavigateToChatbot -> startActivity(
                            ChatbotActivity.createIntent(
                                requireContext(),
                                effect.contractName,
                                getString(effect.contractTypeRes),
                                effect.contractId
                            )
                        )
                        ContractDetailUiEffect.ShowDeleteConfirmation -> showDeleteConfirmation()
                        ContractDetailUiEffect.ShowRenameDialog -> {
                            if (parentFragmentManager.findFragmentByTag(ContractRenameDialog.TAG) == null) {
                                ContractRenameDialog.newInstance(binding.tvContractTitle.text.toString()).show(parentFragmentManager, ContractRenameDialog.TAG)
                            }
                        }
                        ContractDetailUiEffect.ShowExportSheet -> {
                            if (parentFragmentManager.findFragmentByTag(ContractExportBottomSheet.TAG) == null) {
                                ContractExportBottomSheet.newInstance(binding.tvContractTitle.text.toString(), getString(R.string.viewer_metadata, binding.tvFileFormat.text, binding.tvPageCount.text)).show(parentFragmentManager, ContractExportBottomSheet.TAG)
                            }
                        }
                        is ContractDetailUiEffect.ShowMessage -> Toast.makeText(requireContext(), effect.messageRes, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun openAnalysis(effect: ContractDetailUiEffect.NavigateToAnalysis) {
        val app = requireActivity().application as LegalLensApplication
        val repository = app.analysisIdentityRepository
        val session = repository.sessionKey()
        if (session.isNullOrBlank()) {
            Toast.makeText(requireContext(), R.string.auth_backend_not_configured, Toast.LENGTH_LONG).show()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val latest = try { repository.latest(effect.contractId, session) }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: com.example.lagallens.domain.repository.AnalysisContentUnavailableException) {
                if (repository.sessionKey() == session && isAdded) Toast.makeText(requireContext(), R.string.analysis_result_unavailable, Toast.LENGTH_LONG).show()
                return@launch
            } catch (_: Exception) {
                if (repository.sessionKey() == session && isAdded) Toast.makeText(requireContext(), R.string.analysis_storage_error, Toast.LENGTH_LONG).show()
                return@launch
            }
            if (repository.sessionKey() != session || !isAdded) return@launch
            fun startNew() {
                if (repository.sessionKey() != session || !isAdded) return
                startActivity(AnalysisStartActivity.createIntent(requireContext(), effect.documentName,
                    effect.documentMetadata, effect.contractId, effect.documentId))
            }
            if (latest == null) { startNew(); return@launch }
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setMessage(R.string.analysis_existing_result_prompt)
                .setPositiveButton(R.string.analysis_open_existing) { _, _ ->
                    if (repository.sessionKey() == session && isAdded) {
                        val target = com.example.lagallens.presentation.feature.analysis.result.ui.AnalysisResultActivity.createIntent(
                            requireContext(), effect.documentName, effect.documentMetadata)
                        target.putExtra(com.example.lagallens.presentation.feature.analysis.processing.ui.AnalysisProcessingActivity.EXTRA_RESULT_ID, latest.resultId)
                        target.putExtra(com.example.lagallens.presentation.feature.analysis.processing.ui.AnalysisProcessingActivity.EXTRA_SESSION_KEY, session)
                        startActivity(target)
                    }
                }
                .setNegativeButton(R.string.analysis_start_new) { _, _ -> startNew() }
                .setNeutralButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun render(state: ContractDetailUiState) {
        binding.tvContractTitle.text = state.renamedTitle.orEmpty()
        binding.tvContractType.setText(state.contractTypeRes)
        binding.tvStatus.setText(state.statusRes)
        binding.tvRisk.setText(state.riskRes)
        val dateFormat = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM)
        binding.tvCreatedAt.text = state.createdAtMillis.takeIf { it > 0 }?.let { dateFormat.format(java.util.Date(it)) } ?: UNKNOWN_VALUE
        binding.tvUpdatedAt.text = state.updatedAtMillis.takeIf { it > 0 }?.let { dateFormat.format(java.util.Date(it)) } ?: UNKNOWN_VALUE
        binding.tvPageCount.text = state.pageCount?.let { getString(R.string.contract_page_count_format, it) } ?: UNKNOWN_VALUE
        binding.tvFileFormat.text = state.fileFormat.ifBlank { UNKNOWN_VALUE }
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
        private const val UNKNOWN_VALUE = "\u2014"
        const val ARG_CONTRACT_ID = "contractId"
    }
}
