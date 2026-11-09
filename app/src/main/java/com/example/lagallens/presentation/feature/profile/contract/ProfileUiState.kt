package com.example.lagallens.presentation.feature.profile.contract

import androidx.annotation.StringRes
import com.example.lagallens.R

data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val photoUri: String? = null,
    val isBackendUnavailable: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    @param:StringRes val statusRes: Int = R.string.profile_active,
    /** Null until loaded, or when the source is unavailable. */
    val contractCount: Int? = null,
    val analysisCount: Int? = null,
    val chatCount: Int? = null
)
