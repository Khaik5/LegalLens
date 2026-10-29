package com.example.lagallens

import android.os.SystemClock
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.viewpager2.widget.ViewPager2
import com.example.lagallens.presentation.feature.onboarding.ui.OnboardingActivity
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
class OnboardingActivityTest {
    @Test
    fun onboardingContentIsDisplayed() {
        ActivityScenario.launch(OnboardingActivity::class.java)

        onView(withId(R.id.onboarding_logo)).check(matches(isDisplayed()))
        onView(withId(R.id.onboarding_title))
            .check(matches(withText(R.string.onboarding_title_search)))
        onView(withId(R.id.onboarding_next))
            .check(matches(withText(R.string.onboarding_next)))
    }

    @Test
    fun indicatorFollowsPagerSelectionInBothDirections() {
        ActivityScenario.launch(OnboardingActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertSelectedIndicator(activity, selectedIndex = 0)
                activity.findViewById<ViewPager2>(R.id.onboarding_pager).setCurrentItem(1, false)
            }
            SystemClock.sleep(300)
            scenario.onActivity { activity ->
                assertSelectedIndicator(activity, selectedIndex = 1)
                activity.findViewById<ViewPager2>(R.id.onboarding_pager).setCurrentItem(2, false)
            }
            SystemClock.sleep(300)
            scenario.onActivity { activity ->
                assertSelectedIndicator(activity, selectedIndex = 2)
                activity.findViewById<ViewPager2>(R.id.onboarding_pager).setCurrentItem(1, false)
            }
            SystemClock.sleep(300)
            scenario.onActivity { activity ->
                assertSelectedIndicator(activity, selectedIndex = 1)
            }
        }
    }

    @Test
    fun nextButtonUpdatesTheSelectedIndicator() {
        ActivityScenario.launch(OnboardingActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<View>(R.id.onboarding_next).performClick()
            }
            SystemClock.sleep(500)
            scenario.onActivity { activity ->
                assertSelectedIndicator(activity, selectedIndex = 1)
            }
        }
    }

    @Test
    fun skipFinishesOnboardingFromAnyPage() {
        (0..2).forEach { pageIndex ->
            skipOnboardingFromPage(pageIndex)
        }
    }

    private fun skipOnboardingFromPage(pageIndex: Int) {
        ActivityScenario.launch(OnboardingActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<ViewPager2>(R.id.onboarding_pager).setCurrentItem(pageIndex, false)
                activity.findViewById<View>(R.id.onboarding_skip).performClick()
            }
            repeat(20) {
                if (scenario.state != Lifecycle.State.DESTROYED) SystemClock.sleep(50)
            }
            assertEquals(Lifecycle.State.DESTROYED, scenario.state)
        }
    }

    private fun assertSelectedIndicator(activity: OnboardingActivity, selectedIndex: Int) {
        val activeWidth = activity.resources.getDimensionPixelSize(R.dimen.onboarding_dot_active_width)
        val inactiveWidth = activity.resources.getDimensionPixelSize(R.dimen.onboarding_dot_size)
        val dots = listOf(
            activity.findViewById<View>(R.id.onboarding_dot_one),
            activity.findViewById(R.id.onboarding_dot_two),
            activity.findViewById(R.id.onboarding_dot_three)
        )

        dots.forEachIndexed { index, dot ->
            assertEquals(if (index == selectedIndex) activeWidth else inactiveWidth, dot.layoutParams.width)
        }
        assertTrue(dots.count { it.layoutParams.width == activeWidth } == 1)
    }
}
