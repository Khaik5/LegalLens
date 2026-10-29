package com.example.lagallens.presentation.feature.upload.progress.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.databinding.ActivityUploadProgressBinding
import com.example.lagallens.R
import com.example.lagallens.presentation.common.utils.animateContentEntrance
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import com.example.lagallens.presentation.feature.camera.ocr.ui.OcrProcessingActivity
import com.example.lagallens.presentation.feature.upload.progress.contract.UploadProgressEffect
import com.example.lagallens.presentation.feature.upload.progress.contract.UploadProgressEvent
import com.example.lagallens.presentation.feature.upload.progress.contract.UploadProgressUiState
import com.example.lagallens.presentation.feature.upload.progress.viewmodel.UploadProgressViewModel
import kotlinx.coroutines.launch

class UploadProgressActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUploadProgressBinding
    private val viewModel by viewModels<UploadProgressViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityUploadProgressBinding.inflate(layoutInflater)
        setContentView(binding.root)
        listOf(binding.uploadProgressBack, binding.uploadProgressCancel).forEach { it.bindPressFeedback() }
        if (savedInstanceState == null) binding.uploadProgressFileCard.doOnPreDraw {
            it.animateContentEntrance(resources.getDimension(R.dimen.dp_8))
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.uploadProgressRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        bindEvents()
        observeViewModel()
        viewModel.onEvent(
            UploadProgressEvent.ScreenOpened(
                fileName = intent.getStringExtra(EXTRA_FILE_NAME).orEmpty(),
                fileDetails = intent.getStringExtra(EXTRA_FILE_DETAILS).orEmpty(),
                documentUri = intent.getStringExtra(EXTRA_DOCUMENT_URI),
                documentId = intent.getStringExtra(EXTRA_DOCUMENT_ID).orEmpty()
            )
        )
    }

    private fun bindEvents() {
        binding.uploadProgressBack.setOnClickListener {
            viewModel.onEvent(UploadProgressEvent.CancelClicked)
        }
        binding.uploadProgressCancel.setOnClickListener {
            viewModel.onEvent(UploadProgressEvent.CancelClicked)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.effects.collect(::handleEffect) }
            }
        }
    }

    private fun render(state: UploadProgressUiState) = with(binding) {
        uploadProgressIndicator.progress = state.progress
        uploadProgressPercentage.text = getString(com.example.lagallens.R.string.upload_progress_percentage, state.progress)
        uploadProgressFileName.text = state.fileName
        uploadProgressFileDetails.text = state.fileDetails
    }

    private fun handleEffect(effect: UploadProgressEffect) {
        when (effect) {
            UploadProgressEffect.NavigateBack -> finish()
            is UploadProgressEffect.NavigateToOcrProcessing -> {
                startActivity(
                    OcrProcessingActivity.createIntent(
                        this,
                        effect.fileName,
                        effect.fileDetails,
                        shouldSimulateFailure = false,
                        documentUri = effect.documentUri,
                        documentId = effect.documentId
                    )
                )
                finish()
            }
        }
    }

    companion object {
        private const val EXTRA_FILE_NAME = "extra_file_name"
        private const val EXTRA_FILE_DETAILS = "extra_file_details"
        private const val EXTRA_DOCUMENT_URI = "extra_document_uri"
        private const val EXTRA_DOCUMENT_ID = "extra_document_id"

        fun newIntent(context: Context, fileName: String, fileDetails: String, documentUri: String? = null, documentId: String = java.util.UUID.randomUUID().toString()): Intent {
            return Intent(context, UploadProgressActivity::class.java)
                .putExtra(EXTRA_FILE_NAME, fileName)
                .putExtra(EXTRA_FILE_DETAILS, fileDetails)
                .putExtra(EXTRA_DOCUMENT_URI, documentUri)
                .putExtra(EXTRA_DOCUMENT_ID, documentId)
        }
    }
}
