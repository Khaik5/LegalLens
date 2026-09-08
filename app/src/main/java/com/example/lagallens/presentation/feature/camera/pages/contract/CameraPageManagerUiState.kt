package com.example.lagallens.presentation.feature.camera.pages.contract

import com.example.lagallens.presentation.feature.camera.core.ScanPage

data class CameraPageManagerUiState(
    val pages: List<ScanPage> = emptyList()
)
