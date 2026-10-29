package com.example.lagallens.presentation.feature.onboarding.utils

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.drawable.GradientDrawable
import android.view.View
import com.example.lagallens.presentation.common.utils.MotionTokens

object OnboardingIndicatorMotion {
    private const val INDICATOR_DURATION_MILLIS = 220L
    private val colorEvaluator = ArgbEvaluator()

    fun applyState(view: View, width: Int, color: Int) {
        updateWidth(view, width)
        updateColor(view, color)
    }

    fun animateToState(
        view: View,
        targetWidth: Int,
        startColor: Int,
        targetColor: Int,
        onColorUpdated: (Int) -> Unit
    ): ValueAnimator? {
        if (!MotionTokens.areAnimationsEnabled() || !view.isAttachedToWindow) {
            applyState(view, targetWidth, targetColor)
            onColorUpdated(targetColor)
            return null
        }

        val startWidth = view.layoutParams.width
        return ValueAnimator.ofFloat(0f, 1f).apply {
            duration = INDICATOR_DURATION_MILLIS
            interpolator = MotionTokens.fastOutSlowIn
            addUpdateListener { animator ->
                val fraction = animator.animatedFraction
                val width = startWidth + ((targetWidth - startWidth) * fraction).toInt()
                val color = colorEvaluator.evaluate(fraction, startColor, targetColor) as Int
                updateWidth(view, width)
                updateColor(view, color)
                onColorUpdated(color)
            }
            start()
        }
    }

    private fun updateWidth(view: View, width: Int) {
        if (view.layoutParams.width != width) {
            view.layoutParams = view.layoutParams.apply { this.width = width }
        }
    }

    private fun updateColor(view: View, color: Int) {
        val drawable = view.background.mutate()
        if (drawable is GradientDrawable) {
            drawable.setColor(color)
        } else {
            drawable.setTint(color)
        }
        view.background = drawable
    }
}
