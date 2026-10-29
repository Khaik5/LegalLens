package com.example.lagallens

import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lagallens.presentation.common.utils.animateContentEntrance
import com.example.lagallens.presentation.common.utils.animatePressFeedback
import com.example.lagallens.presentation.feature.home.ui.HomeActivity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MotionExtensionsTest {
    @Test fun pressingDuringEntranceRestoresContentPositionAndOpacity() {
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val content = activity.findViewById<FrameLayout>(android.R.id.content)
                val view = View(activity)
                content.addView(view, FrameLayout.LayoutParams(10, 10))
                view.translationY = 7f
                view.animateContentEntrance(20f, startDelayMillis = 500L)
                view.animatePressFeedback(true)
                assertEquals(1f, view.alpha, 0f)
                assertEquals(7f, view.translationY, 0f)
                view.animate().cancel()
                content.removeView(view)
            }
        }
    }
}
