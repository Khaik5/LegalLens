package com.example.lagallens.di

import com.example.lagallens.BuildConfig

enum class AppDataMode { DEMO, REAL }

object DataModeConfig {
    val current: AppDataMode
        get() = when (BuildConfig.DATA_MODE.uppercase()) {
            "DEMO" -> AppDataMode.DEMO
            "REAL" -> AppDataMode.REAL
            else -> error("Unsupported Legal Lens data mode: ${BuildConfig.DATA_MODE}")
        }
}
