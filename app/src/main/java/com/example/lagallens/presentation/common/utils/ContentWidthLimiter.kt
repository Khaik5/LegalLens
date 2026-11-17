package com.example.lagallens.presentation.common.utils

import android.app.Activity
import android.app.Application
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import com.example.lagallens.R
import com.example.lagallens.presentation.feature.camera.capture.ui.CameraCaptureActivity

/**
 * Keeps phone layouts at a readable width on wide windows (foldables, tablets, split screen) by centring the
 * screen content instead of stretching it edge to edge.
 */
object ContentWidthLimiter : Application.ActivityLifecycleCallbacks {
    private const val MAX_CONTENT_WIDTH_DP = 480

    override fun onActivityStarted(activity: Activity) {
        if (activity is CameraCaptureActivity) return
        val density = activity.resources.displayMetrics.density
        val maxWidthPx = (MAX_CONTENT_WIDTH_DP * density).toInt()
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val screen = content.getChildAt(0) ?: return
        if (activity.resources.configuration.screenWidthDp <= MAX_CONTENT_WIDTH_DP) return
        val params = screen.layoutParams as? FrameLayout.LayoutParams ?: return
        if (params.width == maxWidthPx) return
        params.width = maxWidthPx
        params.gravity = Gravity.CENTER_HORIZONTAL
        screen.layoutParams = params
        activity.window.setBackgroundDrawable(ColorDrawable(activity.getColor(R.color.legal_lens_background)))
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
