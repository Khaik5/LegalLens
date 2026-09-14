package com.example.lagallens.presentation.feature.upload.ui

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
class UploadSourceActivityTest {
    @Test
    fun displaysAllUploadSourceOptions() {
        ActivityScenario.launch(UploadSourceActivity::class.java).use {
            onView(withText(R.string.upload_source_title)).check(matches(isDisplayed()))
            onView(withId(R.id.upload_source_pdf)).check(matches(isDisplayed()))
            onView(withId(R.id.upload_source_docx)).check(matches(isDisplayed()))
            onView(withId(R.id.upload_source_gallery)).check(matches(isDisplayed()))
            onView(withId(R.id.upload_source_camera)).check(matches(isDisplayed()))
        }
    }
}
