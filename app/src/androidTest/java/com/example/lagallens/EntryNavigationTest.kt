package com.example.lagallens

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lagallens.presentation.feature.analysis.result.ui.AnalysisResultActivity
import com.example.lagallens.presentation.feature.ocr.saved.ui.OcrSavedActivity
import com.example.lagallens.presentation.feature.onboarding.ui.OnboardingActivity
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EntryNavigationTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test fun onboardingSkipOpensWelcome() {
        ActivityScenario.launch(OnboardingActivity::class.java).use {
            onView(withId(R.id.onboarding_skip)).perform(click())
            android.os.SystemClock.sleep(2000)
            onView(withId(R.id.loginButton)).check(matches(isDisplayed()))
        }
    }

    @Test fun savedOcrReturnOpensHome() {
        val intent = OcrSavedActivity.createIntent(context, "sample.pdf", "1", "OCR")
        ActivityScenario.launch<OcrSavedActivity>(intent).use {
            onView(withId(R.id.ocr_saved_return)).perform(click())
            onView(withId(R.id.mainRoot)).check(matches(isDisplayed()))
        }
    }

    @Test fun analysisFromOcrSystemBackOpensHome() {
        val intent = AnalysisResultActivity.createIntent(context, "sample.pdf", "PDF").putExtra("analysis_return_home", true)
        ActivityScenario.launch<AnalysisResultActivity>(intent).use {
            pressBack()
            onView(withId(R.id.mainRoot)).check(matches(isDisplayed()))
        }
    }
}
