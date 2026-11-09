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
import com.example.lagallens.presentation.feature.documentviewer.contract.DocumentViewerStatus
import com.example.lagallens.presentation.feature.documentviewer.contract.DocumentViewerUiEffect
import com.example.lagallens.presentation.feature.documentviewer.contract.DocumentViewerUiEvent
import com.example.lagallens.presentation.feature.documentviewer.contract.DocumentViewerUiState
import com.example.lagallens.presentation.feature.documentviewer.viewmodel.DocumentViewerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DocumentViewerActivity : AppCompatActivity() {
    private val app get() = application as com.example.lagallens.LegalLensApplication
    private var ownerSession: String? = null
    private fun validOwner(): Boolean = app.currentSessionKey() == ownerSession &&
        (!com.example.lagallens.presentation.feature.chatbot.draft.ui.DraftSessionGuard.isBound(intent) ||
            com.example.lagallens.presentation.feature.chatbot.draft.ui.DraftSessionGuard.valid(app, intent))
    override fun onStart() { super.onStart(); if (!validOwner()) finish() }
    private lateinit var binding: ActivityDocumentViewerBinding
    private val viewModel by viewModels<DocumentViewerViewModel>()
    private var descriptor: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    private var documentUri: Uri? = null
    private var renderJob: Job? = null
    private var renderedKey: String? = null
    private var openJob: Job? = null
    private val pdfLock = Any()
    @Volatile private var documentClosed = false

    private val documentPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (!validOwner()) return@registerForActivityResult
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
        ownerSession = savedInstanceState?.getString(KEY_OWNER_SESSION) ?: app.currentSessionKey()
        if (!validOwner()) { finish(); return }
        lifecycleScope.launch { app.observeSessionKeys().collect { if (!validOwner()) finish() } }
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

        val initialUri = savedInstanceState?.getString(KEY_DOCUMENT_URI)?.let(Uri::parse) ?: intent.documentUri()
        val title = intent.getStringExtra(EXTRA_TITLE)
            ?: initialUri?.lastPathSegment
            ?: getString(R.string.viewer_title)
        val metadata = getString(R.string.viewer_metadata, "PDF", getString(R.string.viewer_page_count, 1, 1))

        binding.viewerBack.setOnClickListener { viewModel.onEvent(DocumentViewerUiEvent.BackClicked) }
        binding.viewerRetry.setOnClickListener { if (validOwner()) documentPicker.launch(arrayOf("application/pdf")) }
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
        outState.putString(KEY_OWNER_SESSION, ownerSession)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        documentClosed = true
        openJob?.cancel()
        renderJob?.cancel()
        // This short cleanup outlives lifecycle cancellation and runs behind any native render.
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch { synchronized(pdfLock) { closeDocument() } }
        super.onDestroy()
    }

    private fun openPdf(uri: Uri) {
        if (!validOwner()) return
        documentUri = uri
        renderedKey = null
        viewModel.onEvent(DocumentViewerUiEvent.PdfLoading)
        binding.viewerRetry.isEnabled = false
        val previousOpen = openJob
        previousOpen?.cancel()
        openJob = lifecycleScope.launch {
            previousOpen?.join()
            renderJob?.cancelAndJoin()
            val pageCount = withContext(Dispatchers.IO) {
                synchronized(pdfLock) {
                    closeDocument()
                    if (documentClosed) return@synchronized null
                    try {
                        val file = contentResolver.openFileDescriptor(uri, "r") ?: error("PDF could not be opened")
                        descriptor = file
                        renderer = PdfRenderer(file)
                        renderer?.pageCount
                    } catch (_: Exception) {
                        closeDocument()
                        null
                    }
                }
            }
            if (!validOwner() || documentClosed) return@launch
            if (pageCount == null || pageCount < 1) {
                withContext(Dispatchers.IO) { synchronized(pdfLock) { closeDocument() } }
                documentUri = null
                viewModel.onEvent(DocumentViewerUiEvent.PdfLoadFailed)
                return@launch
            }
            val displayName = withContext(Dispatchers.IO) { queryDisplayName(uri) }
                ?: binding.viewerDocumentTitle.text.toString()
            val metadata = getString(R.string.viewer_metadata, "PDF", getString(R.string.viewer_page_count, pageCount, pageCount))
            viewModel.onEvent(DocumentViewerUiEvent.PdfLoaded(displayName, metadata, pageCount))
        }
    }

    private fun queryDisplayName(uri: Uri): String? = runCatching {
        contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()

    private fun render(state: DocumentViewerUiState) = with(binding) {
        if (!validOwner()) return@with
        viewerDocumentTitle.text = state.title
        viewerDocumentMetadata.text = state.metadata
        viewerContent.isVisible = state.status == DocumentViewerStatus.READY
        viewerLoading.isVisible = state.status == DocumentViewerStatus.LOADING
        viewerError.isVisible = state.status == DocumentViewerStatus.ERROR
        viewerRetry.setText(R.string.viewer_choose_pdf)
        viewerRetry.isEnabled = state.status != DocumentViewerStatus.LOADING
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
        if (!validOwner()) return
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
            val bitmap = synchronized(pdfLock) { runCatching {
                if (documentClosed || renderer !== currentRenderer) return@runCatching null
                val page = currentRenderer.openPage(state.currentPage - 1)
                try {
                    val availableWidth = (viewportWidth - horizontalMargin).coerceAtLeast(1)
                    val desiredWidth = (availableWidth * state.zoomPercent / 100f).coerceAtLeast(1f)
                    val ratio = page.height.toFloat() / page.width.coerceAtLeast(1)
                    val targetWidth = minOf(desiredWidth, kotlin.math.sqrt(8_000_000f / ratio.coerceAtLeast(0.001f))).toInt().coerceIn(1, 4096)
                    val targetHeight = (targetWidth * ratio).toInt().coerceIn(1, 8192)
                    Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888).also {
                        page.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }
                } finally {
                    page.close()
                }
            }.getOrNull() }
            if (!kotlinx.coroutines.currentCoroutineContext().isActive) { bitmap?.recycle(); return@launch }
            withContext(Dispatchers.Main) {
                if (validOwner() && !isFinishing && documentUri == uri && renderedKey == key && bitmap != null) {
                    binding.viewerPage.setImageBitmap(bitmap)
                    binding.viewerPage.layoutParams = binding.viewerPage.layoutParams.apply {
                        width = bitmap.width
                        height = bitmap.height
                    }
                    binding.viewerPageContainer.layoutParams = binding.viewerPageContainer.layoutParams.apply {
                        width = bitmap.width
                        height = bitmap.height
                    }
                } else if (validOwner() && renderedKey == key && bitmap == null) {
                    viewModel.onEvent(DocumentViewerUiEvent.PdfLoadFailed)
                } else { bitmap?.recycle() }
            }
        }
    }

    private fun shareDocument() {
        if (!validOwner()) return
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
        private const val KEY_OWNER_SESSION = "viewer_owner_session"

        fun createIntent(context: android.content.Context, contractId: String, title: String? = null): Intent =
            Intent(context, DocumentViewerActivity::class.java).putExtra(EXTRA_CONTRACT_ID, contractId).putExtra(EXTRA_TITLE, title)
                .also { com.example.lagallens.presentation.feature.chatbot.draft.ui.DraftSessionGuard.stampIfParentBound(context, it) }

        fun createIntent(context: android.content.Context, documentUri: Uri, title: String): Intent =
            Intent(context, DocumentViewerActivity::class.java)
                .putExtra(EXTRA_DOCUMENT_URI, documentUri)
                .putExtra(EXTRA_TITLE, title)
                .also { com.example.lagallens.presentation.feature.chatbot.draft.ui.DraftSessionGuard.stampIfParentBound(context, it) }
    }
}
