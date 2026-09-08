package com.example.lagallens

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
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
}
