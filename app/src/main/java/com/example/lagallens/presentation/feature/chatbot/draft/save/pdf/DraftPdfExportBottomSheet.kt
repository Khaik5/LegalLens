package com.example.lagallens.presentation.feature.chatbot.draft.save.pdf

import android.app.Application
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.databinding.BottomSheetDraftPdfExportBinding
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftUiState
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.domain.model.DraftStage
import com.example.lagallens.presentation.feature.documentviewer.ui.DocumentViewerActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File

internal enum class DraftPdfStatus { OPTIONS, LOADING, ERROR, READY }
internal data class DraftPdfState(val status: DraftPdfStatus = DraftPdfStatus.OPTIONS, val filePath: String? = null, val saving: Boolean = false, val title: String = "")
internal sealed interface DraftPdfEffect {
    data class Share(val filePath: String) : DraftPdfEffect
    data object Saved : DraftPdfEffect
    data object SaveFailed : DraftPdfEffect
}

internal class DraftPdfExportViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LegalLensApplication
    private val ownerSession = app.currentSessionKey()
    fun isOwnerSession() = ownerSession == app.currentSessionKey()
    private val state = MutableStateFlow(DraftPdfState())
    val uiState = state.asStateFlow()
    private val effectChannel = Channel<DraftPdfEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()
    private var draftKey = ChatbotDraftUiState.APPROVED_AI_VERSION

    init { viewModelScope.launch { app.observeSessionKeys().collect { if (it != ownerSession) {
        draftKey = ChatbotDraftUiState.APPROVED_AI_VERSION
        state.value = DraftPdfState()
        while (effectChannel.tryReceive().isSuccess) { }
    } } } }

    fun setVersion(value: String) { if (isOwnerSession()) draftKey = value }

    fun export(shareWhenReady: Boolean = false) {
        if (!isOwnerSession()) return
        val current = state.value
        if (current.status == DraftPdfStatus.LOADING) return
        if (current.status == DraftPdfStatus.READY) {
            if (shareWhenReady) current.filePath?.let { effectChannel.trySend(DraftPdfEffect.Share(it)) }
            return
        }
        val repository = try { app.draftRepository.forSession(ownerSession) } catch (_: Exception) { return }
        val capturedDraftKey = draftKey
        state.update { it.copy(status = DraftPdfStatus.LOADING, saving = false) }
        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    val jobContext = currentCoroutineContext()
                    val guard: (() -> Unit) -> Unit = { action -> jobContext.ensureActive(); app.withSession(ownerSession, action) }
                    guard { }
                    val stage = DraftStage.fromLegacyKey(capturedDraftKey)
                    val draft = repository.getDraft(capturedDraftKey) ?: stage?.let { repository.getLatestDraft(it) }
                        ?: error("Saved draft not found")
                    check(draft.content.isNotBlank())
                    draft to DraftPdfDocumentWriter.generate(getApplication(), draft.title, draft.content, guard)
                }
                if (isOwnerSession()) {
                    state.update { it.copy(status = DraftPdfStatus.READY, filePath = file.second.absolutePath, title = file.first.title) }
                    if (shareWhenReady) effectChannel.send(DraftPdfEffect.Share(file.second.absolutePath))
                } else {
                    file.second.delete()
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (exception: Exception) {
                android.util.Log.e("DraftPdfExport", exception.javaClass.simpleName)
                if (isOwnerSession()) state.update { it.copy(status = DraftPdfStatus.ERROR, filePath = null) }
            }
        }
    }

    fun saveTo(uri: Uri) {
        if (!isOwnerSession()) return
        val filePath = state.value.filePath ?: return
        if (state.value.saving) return
        state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val saved = try {
                withContext(Dispatchers.IO) {
                    val jobContext = currentCoroutineContext()
                    val guard: (() -> Unit) -> Unit = { action -> jobContext.ensureActive(); app.withSession(ownerSession, action) }
                    DraftPdfDocumentWriter.copyToUri(getApplication(), File(filePath), uri, guard)
                }
                true
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Exception) {
                false
            }
            if (isOwnerSession()) {
                state.update { it.copy(saving = false) }
                effectChannel.send(if (saved) DraftPdfEffect.Saved else DraftPdfEffect.SaveFailed)
            }
        }
    }
}

class DraftPdfExportBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetDraftPdfExportBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModels<DraftPdfExportViewModel>()
    private val savePdfLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) viewModel.saveTo(uri)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = BottomSheetDialog(requireContext(), theme).apply {
        setOnShowListener {
            val sheet = findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet) ?: return@setOnShowListener
            sheet.setBackgroundColor(Color.TRANSPARENT)
            BottomSheetBehavior.from(sheet).state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetDraftPdfExportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if ((activity as? com.example.lagallens.presentation.feature.chatbot.draft.ui.DraftSessionActivity)?.isDraftSessionValid() != true) {
            dismissAllowingStateLoss()
            return
        }
        val version = arguments?.getString(KEY_VERSION) ?: ChatbotDraftUiState.APPROVED_AI_VERSION
        viewModel.setVersion(version)
        binding.pdfCreate.setOnClickListener { viewModel.export() }
        binding.pdfShare.setOnClickListener { viewModel.export(shareWhenReady = true) }
        binding.pdfRetry.setOnClickListener { viewModel.export() }
        binding.pdfSaveFileReady.setOnClickListener { launchSavePicker() }
        binding.pdfShareFileReady.setOnClickListener { viewModel.uiState.value.filePath?.let(::sharePdf) }
        binding.pdfOpenPreview.setOnClickListener { openPdfPreview() }
        listOf(binding.pdfLoadingSaveFile, binding.pdfLoadingShareFile, binding.pdfSaveFileError, binding.pdfShareFileError).forEach { it.isEnabled = false }
        binding.pdfLoadingCancel.setOnClickListener { dismiss() }
        binding.pdfCancelOption.setOnClickListener { dismiss() }
        binding.pdfCancelError.setOnClickListener { dismiss() }
        binding.pdfClose.setOnClickListener { dismiss() }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.effects.collect(::handleEffect) }
            }
        }
    }

    private fun render(state: DraftPdfState) = with(binding) {
        if (!viewModel.isOwnerSession()) { dismissAllowingStateLoss(); return@with }
        pdfDocumentTitle.text = state.title.ifBlank { getString(R.string.chatbot_draft_document_name, arguments?.getString(KEY_VERSION) ?: ChatbotDraftUiState.APPROVED_AI_VERSION) }
        pdfOptions.isVisible = state.status == DraftPdfStatus.OPTIONS
        pdfLoading.isVisible = state.status == DraftPdfStatus.LOADING
        pdfError.isVisible = state.status == DraftPdfStatus.ERROR
        pdfReady.isVisible = state.status == DraftPdfStatus.READY
        pdfSaveFileReady.isEnabled = !state.saving
        pdfSaveFileReady.text = getString(if (state.saving) R.string.draft_pdf_saving_file else R.string.draft_pdf_save_file)
    }

    private fun launchSavePicker() {
        if (!viewModel.isOwnerSession()) return
        val state = viewModel.uiState.value
        if (state.saving || state.filePath == null) return
        val version = arguments?.getString(KEY_VERSION) ?: ChatbotDraftUiState.APPROVED_AI_VERSION
        val title = state.title.ifBlank { getString(R.string.chatbot_draft_document_name, version) }
            .replace(Regex("[^\\p{L}\\p{N} ._-]"), "")
            .trim()
            .take(80)
            .ifBlank { getString(R.string.draft_pdf_default_filename) }
        savePdfLauncher.launch("$title.pdf")
    }

    private fun openPdfPreview() {
        if (!viewModel.isOwnerSession()) return
        val state = viewModel.uiState.value
        val path = state.filePath ?: return
        runCatching {
            val file = File(path)
            check(file.isFile && file.length() > 0L)
            val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.draftpdf", file)
            startActivity(DocumentViewerActivity.createIntent(requireContext(), uri, state.title))
            dismiss()
        }.onFailure {
            Toast.makeText(requireContext(), R.string.draft_pdf_error_body, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleEffect(effect: DraftPdfEffect) {
        if (!viewModel.isOwnerSession()) return
        when (effect) {
            is DraftPdfEffect.Share -> sharePdf(effect.filePath)
            DraftPdfEffect.Saved -> Toast.makeText(requireContext(), R.string.draft_pdf_saved_to_device, Toast.LENGTH_SHORT).show()
            DraftPdfEffect.SaveFailed -> Toast.makeText(requireContext(), R.string.draft_pdf_save_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun sharePdf(filePath: String) {
        if (!viewModel.isOwnerSession()) return
        try {
            val file = File(filePath)
            val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.draftpdf", file)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newUri(requireContext().contentResolver, file.name, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(sendIntent, getString(R.string.draft_pdf_share_chooser_title)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.draft_pdf_share_unavailable, Toast.LENGTH_SHORT).show()
        } catch (_: IllegalArgumentException) {
            Toast.makeText(requireContext(), R.string.draft_pdf_share_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }

    companion object {
        const val TAG = "DraftPdfExportBottomSheet"
        private const val KEY_VERSION = "draft_pdf_version"
        fun newInstance(version: String = ChatbotDraftUiState.APPROVED_AI_VERSION) = DraftPdfExportBottomSheet().apply {
            arguments = Bundle().apply { putString(KEY_VERSION, version) }
        }
    }
}
