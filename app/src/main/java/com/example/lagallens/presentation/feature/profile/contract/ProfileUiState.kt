package com.example.lagallens.presentation.feature.profile.contract

import androidx.annotation.StringRes
import com.example.lagallens.R

data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val photoUri: String? = null,
    val isBackendUnavailable: Boolean = false,
    @param:StringRes val statusRes: Int = R.string.profile_active,
    @param:StringRes val contractCountRes: Int = R.string.profile_contract_count,
    @param:StringRes val analysisCountRes: Int = R.string.profile_analysis_count,
    @param:StringRes val chatCountRes: Int = R.string.profile_chat_count
)
