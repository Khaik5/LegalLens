package com.example.lagallens.presentation.feature.contracts.detail.export.ui

import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.LegalLensApplication
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
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContractExportBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetContractExportBinding? = null
    private val binding get() = _binding!!
    private val sessionKey by lazy { arguments?.getString(KEY_SESSION).orEmpty() }
    private val viewModel by viewModels<ContractExportViewModel> {
        ContractExportViewModel.Factory(
            (requireActivity().application as LegalLensApplication).getDocumentFileUseCase,
            arguments?.getString(KEY_CONTRACT_ID).orEmpty(),
            sessionKey
        )
    }

    /** Downloaded copy waiting for the user to pick where to save it. */
    private var pendingSave: String? = null
    private val savePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val source = pendingSave ?: return@registerForActivityResult
        pendingSave = null
        val destination = result.data?.data
        if (result.resultCode == Activity.RESULT_OK && destination != null) {
            copyToDestination(source, destination)
        } else {
            viewModel.onEvent(ContractExportUiEvent.DownloadReleased(source))
        }
    }

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
        pendingSave = savedInstanceState?.getString(KEY_PENDING_SAVE)
        listOf(binding.exportSave, binding.exportShare, binding.exportRetry, binding.exportCancel).forEach { it.bindPressFeedback() }
        binding.exportSave.setOnClickListener { viewModel.onEvent(ContractExportUiEvent.ExportClicked) }
        binding.exportShare.setOnClickListener { viewModel.onEvent(ContractExportUiEvent.ShareClicked) }
        binding.exportRetry.setOnClickListener { viewModel.onEvent(ContractExportUiEvent.RetryClicked) }
        binding.exportCancel.setOnClickListener { viewModel.onEvent(ContractExportUiEvent.CancelClicked) }
        viewModel.onEvent(ContractExportUiEvent.Opened(arguments?.getString(KEY_TITLE).orEmpty(), arguments?.getString(KEY_METADATA).orEmpty()))
        viewLifecycleOwner.lifecycleScope.launch { viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch { viewModel.uiState.collect(::render) }
            launch { viewModel.uiEffect.collect(::handleEffect) }
        } }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        pendingSave?.let { outState.putString(KEY_PENDING_SAVE, it) }
    }

    private fun render(state: ContractExportUiState) = with(binding) {
        exportDocumentTitle.text = state.title
        exportDocumentMetadata.text = state.metadata
        exportDescription.isVisible = state.status == ContractExportStatus.OPTIONS
        exportStatus.isVisible = state.status != ContractExportStatus.OPTIONS
        exportProgress.isVisible = state.status == ContractExportStatus.LOADING
        exportStatusIcon.isVisible = state.status == ContractExportStatus.ERROR
        exportSave.isEnabled = state.status == ContractExportStatus.OPTIONS
        exportShare.isEnabled = state.status == ContractExportStatus.OPTIONS
        exportShare.alpha = if (exportShare.isEnabled) 1f else 0.5f
        exportRetry.isVisible = state.status == ContractExportStatus.ERROR
        when (state.status) {
            ContractExportStatus.LOADING -> { exportStatus.setBackgroundResource(R.drawable.bg_compare_notice); exportStatusTitle.setText(R.string.contract_export_loading); exportStatusDetail.setText(R.string.contract_export_loading_detail) }
            ContractExportStatus.ERROR -> { exportStatus.setBackgroundResource(R.drawable.bg_contract_export_error); exportStatusIcon.setImageResource(R.drawable.ic_chatbot_error); exportStatusTitle.setText(R.string.contract_export_error); exportStatusDetail.setText(R.string.contract_export_error_detail) }
            ContractExportStatus.OPTIONS -> Unit
        }
    }

    private fun handleEffect(effect: ContractExportUiEffect) {
        if (effect is ContractExportUiEffect.Close) {
            dismiss()
            return
        }
        val fileUri = when (effect) {
            is ContractExportUiEffect.SaveFile -> effect.fileUri
            is ContractExportUiEffect.ShareFile -> effect.fileUri
            else -> return
        }
        // A file downloaded for a session that has since signed out is never handed to the user.
        if (!isOwnerSession()) {
            viewModel.onEvent(ContractExportUiEvent.DownloadReleased(fileUri))
            dismiss()
            return
        }
        when (effect) {
            is ContractExportUiEffect.SaveFile -> openSavePicker(effect)
            is ContractExportUiEffect.ShareFile -> share(effect)
            else -> Unit
        }
    }

    private fun openSavePicker(effect: ContractExportUiEffect.SaveFile) {
        pendingSave = effect.fileUri
        try {
            savePicker.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = effect.mimeType
                putExtra(Intent.EXTRA_TITLE, safeFileName(effect.fileName))
            })
        } catch (_: ActivityNotFoundException) {
            pendingSave = null
            viewModel.onEvent(ContractExportUiEvent.DownloadReleased(effect.fileUri))
            Toast.makeText(requireContext(), R.string.contract_export_save_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyToDestination(source: String, destination: Uri) {
        val context = requireContext().applicationContext
        lifecycleScope.launch {
            val saved = try {
                withContext(Dispatchers.IO) {
                    var copied = false
                    try {
                        check(isOwnerSession())
                        val output = checkNotNull(context.contentResolver.openOutputStream(destination)) { "Cannot open destination" }
                        output.use { stream ->
                            val input = checkNotNull(context.contentResolver.openInputStream(source.toUri())) { "Download unavailable" }
                            input.use { it.copyTo(stream) }
                        }
                        copied = true
                    } finally {
                        if (!copied) runCatching { DocumentsContract.deleteDocument(context.contentResolver, destination) }
                    }
                    copied
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                false
            } finally {
                viewModel.onEvent(ContractExportUiEvent.DownloadReleased(source))
            }
            Toast.makeText(context, if (saved) R.string.contract_export_saved else R.string.contract_export_save_failed, Toast.LENGTH_SHORT).show()
            if (saved && isAdded) dismiss()
        }
    }

    /**
     * Shares a copy named after the original file. It stays in the session cache because the receiving app reads
     * it later; the cache is cleared when the session ends.
     */
    private fun share(effect: ContractExportUiEffect.ShareFile) {
        val context = requireContext()
        try {
            val downloaded = File(checkNotNull(effect.fileUri.toUri().path))
            val folder = File(downloaded.parentFile, "share-${System.currentTimeMillis()}").apply { mkdirs() }
            val named = File(folder, safeFileName(effect.fileName))
            check(downloaded.renameTo(named)) { "Cannot prepare the shared file" }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.draftpdf", named)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = effect.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newUri(context.contentResolver, named.name, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(sendIntent, getString(R.string.contract_export_share)))
            dismiss()
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, R.string.contract_export_share_unavailable, Toast.LENGTH_SHORT).show()
        } catch (_: IllegalArgumentException) {
            Toast.makeText(context, R.string.contract_export_share_unavailable, Toast.LENGTH_SHORT).show()
        } catch (_: IllegalStateException) {
            viewModel.onEvent(ContractExportUiEvent.DownloadReleased(effect.fileUri))
            Toast.makeText(context, R.string.contract_export_share_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun isOwnerSession(): Boolean =
        sessionKey.isNotBlank() && (activity?.application as? LegalLensApplication)?.currentSessionKey() == sessionKey

    override fun onDestroyView() { _binding = null; super.onDestroyView() }

    companion object {
        const val TAG = "ContractExportBottomSheet"
        private const val KEY_TITLE = "export_title"
        private const val KEY_METADATA = "export_metadata"
        private const val KEY_CONTRACT_ID = "export_contract_id"
        private const val KEY_SESSION = "export_session"
        private const val KEY_PENDING_SAVE = "export_pending_save"
        private val UNSAFE_FILE_NAME_CHARACTERS = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")

        /** Keeps the original name (Vietnamese letters included) but drops characters a file system rejects. */
        private fun safeFileName(name: String) =
            name.replace(UNSAFE_FILE_NAME_CHARACTERS, "_").trim().take(120).ifBlank { "document" }

        fun newInstance(title: String, metadata: String, contractId: String, sessionKey: String) = ContractExportBottomSheet().apply {
            arguments = bundleOf(
                KEY_TITLE to title,
                KEY_METADATA to metadata,
                KEY_CONTRACT_ID to contractId,
                KEY_SESSION to sessionKey
            )
        }
    }
}
