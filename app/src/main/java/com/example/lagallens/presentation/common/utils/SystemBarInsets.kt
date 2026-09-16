package com.example.lagallens.presentation.common.utils

import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

fun View.applySystemBarInsets() {
    applySystemBarInsets(top = true, bottom = true)
}

fun View.applySystemBarInsets(
    top: Boolean,
    bottom: Boolean,
    expandHeightForBottomInset: Boolean = false
) {
    val initialPadding = Insets.of(paddingLeft, paddingTop, paddingRight, paddingBottom)
    val initialHeight = layoutParams.height
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
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
