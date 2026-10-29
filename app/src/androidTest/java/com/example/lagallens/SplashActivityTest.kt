package com.example.lagallens

import android.os.SystemClock
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lagallens.presentation.feature.splash.ui.SplashActivity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SplashActivityTest {
    @Test
    fun splashContentIsDisplayed() {
        ActivityScenario.launch(SplashActivity::class.java)

        onView(withId(R.id.splash_logo)).check(matches(isDisplayed()))
        onView(withId(R.id.splash_progress)).check(matches(isDisplayed()))
        onView(withId(R.id.splash_status))
            .check(matches(withText(R.string.splash_initializing_security)))
    }

    @Test
    fun splashEntranceMotionPreservesXmlTranslationTargets() {
        ActivityScenario.launch(SplashActivity::class.java).use { scenario ->
            SystemClock.sleep(600)
            scenario.onActivity { activity ->
                assertTranslationY(
                    activity.findViewById(R.id.splash_logo),
                    activity.resources.getDimension(R.dimen.splash_logo_top)
                )
                assertTranslationY(
                    activity.findViewById(R.id.splash_progress),
                    activity.resources.getDimension(R.dimen.splash_progress_top)
                )
                assertTranslationY(
                    activity.findViewById(R.id.splash_status),
                    activity.resources.getDimension(R.dimen.splash_label_top)
                )
            }
        }
    }

    private fun assertTranslationY(view: View, expected: Float) {
        assertEquals(expected, view.translationY, 0.01f)
    }
}
