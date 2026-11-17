package com.example.lagallens.presentation.common.utils

import android.content.Context
import com.example.lagallens.R
import com.example.lagallens.di.AppDataMode
import com.example.lagallens.di.DataModeConfig

/** True when the app shows built-in sample data instead of backend data. */
fun isDemoData(): Boolean = DataModeConfig.current == AppDataMode.DEMO

/** Marks sample content with "[DEMO]"; real content is shown as it is. */
fun Context.demoTagged(text: CharSequence): String =
    if (isDemoData()) getString(R.string.analysis_snapshot_demo_metadata, text) else text.toString()
