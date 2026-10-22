package com.example.lagallens.presentation.common.view

import android.content.Context
import android.graphics.BitmapFactory
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.doOnLayout
import com.example.lagallens.R
import kotlin.math.abs
import kotlin.math.hypot

class FloatingPremiumSticker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {
    private val positionStore = FloatingPremiumStickerPositionStore(context)
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private val edgeMargin = resources.getDimensionPixelSize(R.dimen.floating_premium_sticker_margin)
    private var safeTopInset = 0
    private var downRawX = 0f
    private var downRawY = 0f
    private var downViewX = 0f
    private var downViewY = 0f
    private var isDragging = false
    private var onStickerClick: (() -> Unit)? = null

    init {
        setImageBitmap(BitmapFactory.decodeResource(resources, R.drawable.img_floating_premium_sticker))
        contentDescription = context.getString(R.string.floating_premium_sticker_description)
        elevation = resources.getDimension(R.dimen.dp_8)
        isClickable = true
        isFocusable = true
        doOnLayout { restorePosition() }
    }

    fun setOnStickerClick(listener: () -> Unit) {
        onStickerClick = listener
    }

    fun setSafeTopInset(inset: Int) {
        safeTopInset = inset
        post { restorePosition() }
    }

    override fun performClick(): Boolean {
        super.performClick()
        onStickerClick?.invoke()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> beginTouch(event)
            MotionEvent.ACTION_MOVE -> moveSticker(event)
            MotionEvent.ACTION_UP -> endTouch()
            MotionEvent.ACTION_CANCEL -> cancelTouch()
        }
        return true
    }

    private fun beginTouch(event: MotionEvent) {
        downRawX = event.rawX
        downRawY = event.rawY
        downViewX = x
        downViewY = y
        isDragging = false
        animate().scaleX(PRESSED_SCALE).scaleY(PRESSED_SCALE).setDuration(SCALE_DURATION_MILLIS).start()
    }

    private fun moveSticker(event: MotionEvent) {
        val deltaX = event.rawX - downRawX
        val deltaY = event.rawY - downRawY
        if (!isDragging && hypot(deltaX, deltaY) > touchSlop) {
            isDragging = true
            parent?.requestDisallowInterceptTouchEvent(true)
        }
        if (isDragging) {
            x = (downViewX + deltaX).coerceIn(minimumX(), maximumX())
            y = (downViewY + deltaY).coerceIn(minimumY(), maximumY())
        }
    }

    private fun endTouch() {
        restoreScale()
        if (isDragging) {
            snapToNearestEdge()
        } else {
            performClick()
        }
    }

    private fun cancelTouch() {
        restoreScale()
        if (isDragging) {
            snapToNearestEdge()
        }
    }

    private fun snapToNearestEdge() {
        val targetX = if (abs(x - minimumX()) <= abs(maximumX() - x)) minimumX() else maximumX()
        val targetY = y.coerceIn(minimumY(), maximumY())
        animate()
            .x(targetX)
            .y(targetY)
            .setDuration(SNAP_DURATION_MILLIS)
            .withEndAction { savePosition() }
            .start()
    }

    private fun restorePosition() {
        val parentView = parent as? View ?: return
        if (parentView.width == 0 || parentView.height == 0 || width == 0 || height == 0) return
        val position = positionStore.load()
        x = if (position.edge == FloatingPremiumStickerEdge.LEFT) minimumX() else maximumX()
        y = minimumY() + (maximumY() - minimumY()) * position.verticalFraction
    }

    private fun savePosition() {
        val edge = if (abs(x - minimumX()) <= abs(maximumX() - x)) {
            FloatingPremiumStickerEdge.LEFT
        } else {
            FloatingPremiumStickerEdge.RIGHT
        }
        val verticalRange = maximumY() - minimumY()
        val verticalFraction = if (verticalRange == 0f) 0f else (y - minimumY()) / verticalRange
        positionStore.save(FloatingPremiumStickerPosition(edge, verticalFraction.coerceIn(0f, 1f)))
    }

    private fun restoreScale() {
        animate().scaleX(DEFAULT_SCALE).scaleY(DEFAULT_SCALE).setDuration(SCALE_DURATION_MILLIS).start()
    }

    private fun minimumX() = edgeMargin.toFloat()

    private fun maximumX(): Float {
        val parentView = parent as? View ?: return minimumX()
        return (parentView.width - width - edgeMargin).coerceAtLeast(edgeMargin).toFloat()
    }

    private fun minimumY() = (safeTopInset + edgeMargin).toFloat()

    private fun maximumY(): Float {
        val parentView = parent as? View ?: return minimumY()
        return (parentView.height - height - edgeMargin).coerceAtLeast(minimumY().toInt()).toFloat()
    }

    private companion object {
        const val DEFAULT_SCALE = 1f
        const val PRESSED_SCALE = 0.94f
        const val SCALE_DURATION_MILLIS = 100L
        const val SNAP_DURATION_MILLIS = 180L
    }
}
