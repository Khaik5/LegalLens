package com.example.lagallens.presentation.feature.documentviewer.contract

enum class DocumentViewerStatus { LOADING, READY, ERROR }

data class DocumentViewerUiState(
    val title: String = "",
    val metadata: String = "",
    val pageCount: Int = 1,
    val currentPage: Int = 1,
    val zoomPercent: Int = 100,
    val status: DocumentViewerStatus = DocumentViewerStatus.LOADING,
    val isInitialized: Boolean = false
)
