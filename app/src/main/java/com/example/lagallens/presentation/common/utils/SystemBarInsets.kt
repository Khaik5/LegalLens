package com.example.lagallens.presentation.common.utils

import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

fun View.applySystemBarInsets() {
    applySystemBarInsets(top = true, bottom = true)
}

/**
 * Pads for the system bars and, at the bottom, for the keyboard. Edge-to-edge windows are not resized by
 * `adjustResize`, so without the keyboard inset a bottom-anchored button or the end of a form stays hidden.
 * Pass [top] = false when a parent already pads for the status bar.
 */
fun View.applySystemBarAndKeyboardInsets(top: Boolean = true) {
    val initialPadding = Insets.of(paddingLeft, paddingTop, paddingRight, paddingBottom)
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        val keyboard = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
        view.updatePadding(
            left = initialPadding.left + systemBars.left,
            top = initialPadding.top + if (top) systemBars.top else 0,
            right = initialPadding.right + systemBars.right,
            bottom = initialPadding.bottom + maxOf(systemBars.bottom, keyboard.bottom)
        )
        windowInsets
    }
    ViewCompat.requestApplyInsets(this)
}

fun View.applySystemBarInsets(
    top: Boolean,
    bottom: Boolean,
    expandHeightForBottomInset: Boolean = false
) {
    val initialPadding = Insets.of(paddingLeft, paddingTop, paddingRight, paddingBottom)
    val initialHeight = layoutParams.height
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        view.updatePadding(
            left = initialPadding.left + systemBars.left,
            top = initialPadding.top + if (top) systemBars.top else 0,
            right = initialPadding.right + systemBars.right,
            bottom = initialPadding.bottom + if (bottom) systemBars.bottom else 0
        )
        if (expandHeightForBottomInset && initialHeight > 0) {
            view.layoutParams = view.layoutParams.apply {
                height = initialHeight + systemBars.bottom
            }
        }
        windowInsets
    }
    ViewCompat.requestApplyInsets(this)
}
