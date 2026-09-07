package com.example.lagallens.presentation.feature.upload.error.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lagallens.R
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UploadErrorActivityTest {
    @Test
    fun displaysTheUploadFailureState() {
        ActivityScenario.launch<UploadErrorActivity>(
            UploadErrorActivity.newIntent(
                context = androidx.test.core.app.ApplicationProvider.getApplicationContext()
            )
        ).use {
            onView(withText(R.string.upload_error_title)).check(matches(isDisplayed()))
            onView(withId(R.id.upload_error_heading)).check(matches(isDisplayed()))
            onView(withId(R.id.upload_error_code))
                .check(matches(withText("MÃ LỖI: OCR_READ_FAILED")))
            onView(withId(R.id.upload_error_retry)).check(matches(isDisplayed()))
            onView(withId(R.id.upload_error_back_to_list)).check(matches(isDisplayed()))
        }
    }
}
