package com.example.lagallens.presentation.common.utils

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

/**
 * A context whose strings follow the language chosen in the app. The application context alone keeps the system
 * language, so a ViewModel that builds display text must read it through this.
 */
fun Context.withAppLocale(): Context {
    val locale = AppCompatDelegate.getApplicationLocales()[0] ?: return this
    return createConfigurationContext(Configuration(resources.configuration).apply { setLocale(locale) })
}
