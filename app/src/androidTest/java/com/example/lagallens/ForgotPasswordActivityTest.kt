package com.example.lagallens

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.example.lagallens.presentation.feature.forgotpassword.ui.ForgotPasswordActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ForgotPasswordActivityTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(ForgotPasswordActivity::class.java)

    @Test
    fun rendersAndAcceptsEmailSubmission() {
        onView(withId(R.id.forgotPasswordTitleText))
            .check(matches(withText(R.string.reset_password_title)))
        onView(withId(R.id.emailInput)).check(matches(isDisplayed()))
        onView(withId(R.id.submitButton))
            .check(matches(withText(R.string.send_reset_link)))
        onView(withId(R.id.emailInput)).perform(replaceText("user@example.com"))
        activityRule.scenario.onActivity { activity ->
            activity.findViewById<android.view.View>(R.id.submitButton).performClick()
        }
    }
}
