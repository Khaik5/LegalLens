package com.example.lagallens.core.constants

import com.example.lagallens.BuildConfig

object FeatureFlags {
    /** Drafting a new contract from a chat; off until the backend accepts chat input for generation. */
    val isContractDraftingEnabled: Boolean = BuildConfig.CONTRACT_DRAFTING_ENABLED
}
