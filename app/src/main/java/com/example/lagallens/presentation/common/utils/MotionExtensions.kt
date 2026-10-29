package com.example.lagallens.presentation.common.utils

import android.animation.ValueAnimator
import android.view.MotionEvent
import android.view.View
import android.view.animation.PathInterpolator
import com.example.lagallens.R

object MotionTokens {
    const val FAST_DURATION_MILLIS = 120L
    const val STANDARD_DURATION_MILLIS = 200L
    const val EMPHASIS_DURATION_MILLIS = 280L

    val fastOutSlowIn = PathInterpolator(0.4f, 0f, 0.2f, 1f)
    val linearOutSlowIn = PathInterpolator(0f, 0f, 0.2f, 1f)
    val fastOutLinearIn = PathInterpolator(0.4f, 0f, 1f, 1f)

    fun areAnimationsEnabled(): Boolean = ValueAnimator.areAnimatorsEnabled()
}

fun View.animatePressFeedback(
    isPressed: Boolean,
    pressedScale: Float = 0.97f
) {
    val targetScale = if (isPressed && MotionTokens.areAnimationsEnabled()) pressedScale else 1f
    animate().cancel()
    finishPendingEntrance()
    if (!MotionTokens.areAnimationsEnabled() || !isAttachedToWindow) {
        scaleX = targetScale
        scaleY = targetScale
        return
    }

    animate()
        .scaleX(targetScale)
        .scaleY(targetScale)
        .setStartDelay(0L)
        .setDuration(MotionTokens.FAST_DURATION_MILLIS)
        .setInterpolator(MotionTokens.fastOutSlowIn)
        .start()
}

fun View.bindPressFeedback(pressedScale: Float = 0.97f) {
    setOnTouchListener { _, event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> animatePressFeedback(isPressed = true, pressedScale = pressedScale)
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> animatePressFeedback(isPressed = false, pressedScale = pressedScale)
        }
        false
    }
}

fun View.animateLoadingFeedback(isLoading: Boolean) {
    val targetAlpha = if (isLoading) 0.72f else 1f
    animate().cancel()
    finishPendingEntrance()
    if (!MotionTokens.areAnimationsEnabled() || !isAttachedToWindow) {
        alpha = targetAlpha
        return
    }

    animate()
        .alpha(targetAlpha)
        .setStartDelay(0L)
        .setDuration(MotionTokens.FAST_DURATION_MILLIS)
        .setInterpolator(MotionTokens.fastOutSlowIn)
        .start()
}

fun View.animateContentEntrance(
    translationDistanceY: Float,
    durationMillis: Long = MotionTokens.STANDARD_DURATION_MILLIS,
    startDelayMillis: Long = 0L
) {
    animate().cancel()
    finishPendingEntrance()
    val targetTranslationY = translationY
    if (!MotionTokens.areAnimationsEnabled() || !isAttachedToWindow) {
        alpha = 1f
        translationY = targetTranslationY
        return
    }

    alpha = 0f
    translationY = targetTranslationY + translationDistanceY
    setTag(R.id.motion_entrance_target, targetTranslationY)
    animate()
        .alpha(1f)
        .translationY(targetTranslationY)
        .setDuration(durationMillis)
        .setStartDelay(startDelayMillis)
        .setInterpolator(MotionTokens.linearOutSlowIn)
        .withEndAction { setTag(R.id.motion_entrance_target, null) }
        .start()
}

fun View.cancelMotion() {
    animate().cancel()
    finishPendingEntrance()
}

private fun View.finishPendingEntrance() {
    val target = getTag(R.id.motion_entrance_target) as? Float ?: return
    translationY = target
    alpha = 1f
    setTag(R.id.motion_entrance_target, null)
}
