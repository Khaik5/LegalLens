package com.example.lagallens.presentation.feature.splash.utils

import android.animation.ValueAnimator
import android.os.SystemClock
import android.view.View

object SplashMotion {
    private const val START_PROGRESS = 0
    private const val END_PROGRESS = 100
    private const val PROGRESS_DURATION_MILLIS = 1_800L
    private const val FRAME_DELAY_MILLIS = 16L

    fun startProgress(progressIndicator: View): SplashProgressAnimation? {
        progressIndicator.scaleX = START_PROGRESS.toFloat()
        if (!ValueAnimator.areAnimatorsEnabled()) {
            progressIndicator.scaleX = 1f
            return null
        }

        return SplashProgressAnimation(progressIndicator).also(SplashProgressAnimation::start)
    }

    class SplashProgressAnimation(private val progressIndicator: View) {
        private var startedAtMillis = 0L
        private val updateProgress = object : Runnable {
            override fun run() {
                val elapsedMillis = SystemClock.uptimeMillis() - startedAtMillis
                val progress = (elapsedMillis * END_PROGRESS / PROGRESS_DURATION_MILLIS)
                    .toInt()
                    .coerceAtMost(END_PROGRESS)
                progressIndicator.scaleX = progress / END_PROGRESS.toFloat()

                if (progress < END_PROGRESS) {
                    progressIndicator.postDelayed(this, FRAME_DELAY_MILLIS)
                }
            }
        }

        fun start() {
            startedAtMillis = SystemClock.uptimeMillis()
            progressIndicator.post(updateProgress)
        }

        fun cancel() {
            progressIndicator.removeCallbacks(updateProgress)
        }
    }
}
