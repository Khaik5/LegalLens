package com.example.lagallens.presentation.feature.upload.progress.ui

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
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

@RunWith(AndroidJUnit4::class)
class UploadProgressActivityTest {
    @Test
    fun displaysSelectedFileProcessingState() {
        ActivityScenario.launch<UploadProgressActivity>(
            UploadProgressActivity.newIntent(
                context = androidx.test.core.app.ApplicationProvider.getApplicationContext(),
                fileName = "hop_dong_thue_nha_2026.pdf",
                fileDetails = "2.4 MB · PDF Document"
            )
        ).use {
            onView(withText(R.string.upload_progress_title)).check(matches(isDisplayed()))
            onView(withId(R.id.upload_progress_percentage)).check(matches(withText(object : TypeSafeMatcher<String>() {
                override fun matchesSafely(value: String): Boolean = value.endsWith("%") && value.dropLast(1).toIntOrNull()?.let { it in 0..100 } == true
                override fun describeTo(description: Description) { description.appendText("a percentage from 0 to 100") }
            })))
            onView(withId(R.id.upload_progress_file_name))
                .check(matches(withText("hop_dong_thue_nha_2026.pdf")))
            onView(withId(R.id.upload_progress_cancel)).check(matches(isDisplayed()))
        }
    }
}
