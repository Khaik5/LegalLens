package com.example.lagallens

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lagallens.presentation.feature.main.ui.MainActivity
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardNavigationTest {
    @Test fun viewAllThenHomeReturnsToDashboard() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.tvViewAllContracts)).perform(click())
            onView(withId(R.id.contractAppBar)).check(matches(isDisplayed()))
            onView(withId(R.id.navHome)).perform(click())
            onView(withId(R.id.dashboardGreeting)).check(matches(isDisplayed()))
        }
    }
}
