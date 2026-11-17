package com.example.lagallens.presentation.common.utils

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Pulses the dots of a "still working" footer one after another while [owner] is on screen.
 * Dots are the [View]s with a fixed size; the empty spacer views between them keep their alpha.
 * Nothing moves when the system turns animations off.
 */
fun ViewGroup.pulseDotsWhileStarted(owner: LifecycleOwner) {
    var running: AnimatorSet? = null
    owner.lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            if (!MotionTokens.areAnimationsEnabled()) return
            running = AnimatorSet().apply {
                playTogether(dotViews().mapIndexed { index, dot -> dot.pulse(index * PULSE_STAGGER_MILLIS) })
                start()
            }
        }

        override fun onStop(owner: LifecycleOwner) {
            running?.cancel()
            running = null
            dotViews().forEach { it.alpha = 1f }
        }
    })
}

private fun ViewGroup.dotViews(): List<View> =
    (0 until childCount).map(::getChildAt).filter { it.background != null }

private fun View.pulse(startDelayMillis: Long): Animator = ObjectAnimator.ofFloat(this, View.ALPHA, 1f, PULSE_MIN_ALPHA).apply {
    duration = PULSE_DURATION_MILLIS
    this.startDelay = startDelayMillis
    repeatCount = ValueAnimator.INFINITE
    repeatMode = ValueAnimator.REVERSE
    interpolator = LinearInterpolator()
}

private const val PULSE_DURATION_MILLIS = 500L
private const val PULSE_STAGGER_MILLIS = 170L
private const val PULSE_MIN_ALPHA = 0.25f
