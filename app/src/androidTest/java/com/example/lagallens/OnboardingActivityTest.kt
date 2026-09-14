package com.example.lagallens

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lagallens.presentation.feature.onboarding.ui.OnboardingActivity
import org.junit.Test
import org.junit.runner.RunWith

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
}
