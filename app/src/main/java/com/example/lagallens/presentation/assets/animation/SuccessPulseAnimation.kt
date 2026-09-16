package com.example.lagallens.presentation.assets.animation

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

object SuccessPulseAnimation {
    fun start(target: View): AnimatorSet {
        val scaleX = ObjectAnimator.ofFloat(target, View.SCALE_X, 1f, 1.08f).apply {
            duration = 800
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
        }
        val scaleY = ObjectAnimator.ofFloat(target, View.SCALE_Y, 1f, 1.08f).apply {
            duration = 800
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
        }
        return AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }
}
