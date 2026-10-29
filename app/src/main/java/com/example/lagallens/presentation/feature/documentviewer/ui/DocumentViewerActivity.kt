package com.example.lagallens.presentation.feature.documentviewer.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityDocumentViewerBinding
import com.example.lagallens.presentation.common.utils.animateContentEntrance
import com.example.lagallens.presentation.common.utils.applySystemBarInsets
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import com.example.lagallens.presentation.feature.contracts.contract.ContractListItem
import com.example.lagallens.presentation.feature.documentviewer.contract.DocumentViewerStatus
import com.example.lagallens.presentation.feature.documentviewer.contract.DocumentViewerUiEffect
import com.example.lagallens.presentation.feature.documentviewer.contract.DocumentViewerUiEvent
import com.example.lagallens.presentation.feature.documentviewer.contract.DocumentViewerUiState
import com.example.lagallens.presentation.feature.documentviewer.viewmodel.DocumentViewerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DocumentViewerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDocumentViewerBinding
    private val viewModel by viewModels<DocumentViewerViewModel>()
    private var descriptor: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    private var documentUri: Uri? = null
    private var renderJob: Job? = null
    private var renderedKey: String? = null

    private val documentPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            viewModel.onEvent(DocumentViewerUiEvent.PdfLoadFailed)
        } else {
            runCatching {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            openPdf(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityDocumentViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        listOf(binding.viewerBack, binding.viewerRetry, binding.viewerPrevious, binding.viewerNext, binding.viewerZoomIn, binding.viewerZoomOut, binding.viewerShare).forEach { it.bindPressFeedback() }
        if (savedInstanceState == null) binding.viewerToolbar.doOnPreDraw { it.animateContentEntrance(-resources.getDimension(R.dimen.dp_8)) }
        binding.root.applySystemBarInsets()

        val contract = ContractListItem.allSamples.find { it.id == intent.getStringExtra(EXTRA_CONTRACT_ID) }
        val initialUri = savedInstanceState?.getString(KEY_DOCUMENT_URI)?.let(Uri::parse) ?: intent.documentUri()
        val title = intent.getStringExtra(EXTRA_TITLE)
            ?: contract?.let { getString(it.titleRes) }
            ?: initialUri?.lastPathSegment
            ?: getString(R.string.viewer_title)
        val metadata = contract?.let {
            getString(R.string.viewer_metadata, getString(it.fileFormatRes), getString(it.pageCountRes))
        } ?: getString(R.string.viewer_metadata, "PDF", getString(R.string.viewer_page_count, 1, 1))

        binding.viewerBack.setOnClickListener { viewModel.onEvent(DocumentViewerUiEvent.BackClicked) }
        binding.viewerRetry.setOnClickListener { documentPicker.launch(arrayOf("application/pdf")) }
        binding.viewerShare.setOnClickListener { shareDocument() }
        binding.viewerPrevious.setOnClickListener { viewModel.onEvent(DocumentViewerUiEvent.PreviousClicked) }
        binding.viewerNext.setOnClickListener { viewModel.onEvent(DocumentViewerUiEvent.NextClicked) }
        binding.viewerZoomOut.setOnClickListener { viewModel.onEvent(DocumentViewerUiEvent.ZoomOutClicked) }
        binding.viewerZoomIn.setOnClickListener { viewModel.onEvent(DocumentViewerUiEvent.ZoomInClicked) }
        binding.viewerPageViewport.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            renderedKey = null
            renderPage(viewModel.uiState.value)
        }

        viewModel.onEvent(DocumentViewerUiEvent.Opened(title, metadata, 1))
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.uiEffect.collect { if (it == DocumentViewerUiEffect.NavigateBack) finish() } }
            }
        }
        if (initialUri != null) openPdf(initialUri)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        documentUri?.toString()?.let { outState.putString(KEY_DOCUMENT_URI, it) }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        renderJob?.cancel()
        closeDocument()
        super.onDestroy()
    }

    private fun openPdf(uri: Uri) {
        documentUri = uri
        renderedKey = null
        viewModel.onEvent(DocumentViewerUiEvent.PdfLoading)
        binding.viewerRetry.isEnabled = false
        lifecycleScope.launch {
            renderJob?.cancelAndJoin()
            closeDocument()
            val opened = withContext(Dispatchers.IO) {
                runCatching {
                    val fileDescriptor = contentResolver.openFileDescriptor(uri, "r") ?: error("PDF could not be opened")
                    try {
                        val pdfRenderer = PdfRenderer(fileDescriptor)
                        Triple(fileDescriptor, pdfRenderer, pdfRenderer.pageCount)
                    } catch (exception: Exception) {
                        fileDescriptor.close()
                        throw exception
                    }
                }.getOrNull()
            }
            if (opened == null || opened.third < 1) {
                opened?.second?.close()
                opened?.first?.close()
                documentUri = null
                viewModel.onEvent(DocumentViewerUiEvent.PdfLoadFailed)
                return@launch
            }
            descriptor = opened.first
            renderer = opened.second
            val displayName = queryDisplayName(uri) ?: binding.viewerDocumentTitle.text.toString()
            val metadata = getString(R.string.viewer_metadata, "PDF", getString(R.string.viewer_page_count, opened.third, opened.third))
            viewModel.onEvent(DocumentViewerUiEvent.PdfLoaded(displayName, metadata, opened.third))
        }
    }

    private fun queryDisplayName(uri: Uri): String? = runCatching {
        contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()

    private fun render(state: DocumentViewerUiState) = with(binding) {
        viewerDocumentTitle.text = state.title
        viewerDocumentMetadata.text = state.metadata
        viewerContent.isVisible = state.status == DocumentViewerStatus.READY
        viewerLoading.isVisible = state.status == DocumentViewerStatus.LOADING
        viewerError.isVisible = state.status == DocumentViewerStatus.ERROR
        viewerRetry.setText(R.string.viewer_choose_pdf)
        viewerShare.isEnabled = state.status == DocumentViewerStatus.READY && documentUri != null
        viewerPageCount.text = getString(R.string.viewer_page_count, state.currentPage, state.pageCount)
        viewerPageNumber.text = state.currentPage.toString()
        viewerZoom.text = getString(R.string.viewer_zoom_level, state.zoomPercent)
        viewerPrevious.isEnabled = state.status == DocumentViewerStatus.READY && state.currentPage > 1
        viewerNext.isEnabled = state.status == DocumentViewerStatus.READY && state.currentPage < state.pageCount
        viewerZoomOut.isEnabled = state.status == DocumentViewerStatus.READY && state.zoomPercent > 75
        viewerZoomIn.isEnabled = state.status == DocumentViewerStatus.READY && state.zoomPercent < 150
        renderPage(state)
    }

    private fun renderPage(state: DocumentViewerUiState) {
        val activeRenderer = renderer ?: return
        if (state.status != DocumentViewerStatus.READY || binding.viewerPageViewport.width <= 0) return
        val uri = documentUri ?: return
        val viewportWidth = binding.viewerPageViewport.width
        val horizontalMargin = 2 * resources.getDimensionPixelSize(R.dimen.dp_20)
        val currentRenderer = activeRenderer
        val key = "$uri:${state.currentPage}:${state.zoomPercent}:${binding.viewerPageViewport.width}"
        if (renderedKey == key) return
        renderedKey = key
        renderJob?.cancel()
        renderJob = lifecycleScope.launch(Dispatchers.IO) {
            val bitmap = runCatching {
                val page = currentRenderer.openPage(state.currentPage - 1)
                try {
                    val availableWidth = (viewportWidth - horizontalMargin).coerceAtLeast(1)
                    val targetWidth = (availableWidth * state.zoomPercent / 100f).toInt().coerceAtLeast(1)
                    val targetHeight = (targetWidth * page.height.toFloat() / page.width).toInt().coerceAtLeast(1)
                    Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888).also {
                        page.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }
                } finally {
                    page.close()
                }
            }.getOrNull()
            withContext(Dispatchers.Main) {
                if (!isFinishing && documentUri == uri && bitmap != null) {
                    binding.viewerPage.setImageBitmap(bitmap)
                    binding.viewerPage.layoutParams = binding.viewerPage.layoutParams.apply {
                        width = bitmap.width
                        height = bitmap.height
                    }
                    binding.viewerPageContainer.layoutParams = binding.viewerPageContainer.layoutParams.apply {
                        width = bitmap.width
                        height = bitmap.height
                    }
                } else if (bitmap == null) {
                    viewModel.onEvent(DocumentViewerUiEvent.PdfLoadFailed)
                }
            }
        }
    }

    private fun shareDocument() {
        val uri = documentUri ?: return
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newUri(contentResolver, binding.viewerDocumentTitle.text, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(sendIntent, getString(R.string.viewer_share)))
        } catch (_: ActivityNotFoundException) {
            viewModel.onEvent(DocumentViewerUiEvent.PdfLoadFailed)
        }
    }

    private fun closeDocument() {
        runCatching { renderer?.close() }
        runCatching { descriptor?.close() }
        renderer = null
        descriptor = null
    }

    @Suppress("DEPRECATION")
    private fun Intent.documentUri(): Uri? = if (Build.VERSION.SDK_INT >= 33) {
        getParcelableExtra(EXTRA_DOCUMENT_URI, Uri::class.java)
    } else {
        getParcelableExtra(EXTRA_DOCUMENT_URI)
    }

    companion object {
        private const val EXTRA_CONTRACT_ID = "viewer_contract_id"
        private const val EXTRA_DOCUMENT_URI = "viewer_document_uri"
        private const val EXTRA_TITLE = "viewer_document_title"
        private const val KEY_DOCUMENT_URI = "viewer_saved_uri"

        fun createIntent(context: android.content.Context, contractId: String): Intent =
            Intent(context, DocumentViewerActivity::class.java).putExtra(EXTRA_CONTRACT_ID, contractId)

        fun createIntent(context: android.content.Context, documentUri: Uri, title: String): Intent =
            Intent(context, DocumentViewerActivity::class.java)
                .putExtra(EXTRA_DOCUMENT_URI, documentUri)
                .putExtra(EXTRA_TITLE, title)
    }
}
