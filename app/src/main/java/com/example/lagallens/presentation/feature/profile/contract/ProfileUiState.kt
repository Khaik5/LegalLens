package com.example.lagallens.presentation.feature.profile.contract

import androidx.annotation.StringRes
import com.example.lagallens.R

data class ProfileUiState(
    val name: String = "Nguyễn Văn A",
    val email: String = "nguyenvana@legallens.vn",
    val phone: String = "0901 234 567",
    @StringRes val statusRes: Int = R.string.profile_active,
    @StringRes val contractCountRes: Int = R.string.profile_contract_count,
    @StringRes val analysisCountRes: Int = R.string.profile_analysis_count,
    @StringRes val chatCountRes: Int = R.string.profile_chat_count
)


