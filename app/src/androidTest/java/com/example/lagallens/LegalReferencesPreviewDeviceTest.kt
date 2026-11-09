package com.example.lagallens

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lagallens.presentation.feature.analysis.legalreferences.ui.AnalysisLegalReferencesActivity
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LegalReferencesPreviewDeviceTest {
    @Test fun lockedPreviewOpensDetailAndUnlockConfirmation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = AnalysisTestFixture.scoped(AnalysisLegalReferencesActivity.createIntent(context, "sample", "PDF"), unlocked = false)
        ActivityScenario.launch<AnalysisLegalReferencesActivity>(intent).use {
            Thread.sleep(1500)
            onView(withId(R.id.analysisLegalReferencesDemo)).check(matches(isDisplayed()))
            onView(withId(R.id.analysisLegalReferencesUnlock)).check(matches(isDisplayed()))
            onView(withId(R.id.analysisLegalReferenceFull)).perform(click())
            Thread.sleep(1000)
            onView(withId(R.id.analysisLegalReferenceDetailLawTitle)).check(matches(isDisplayed()))
            androidx.test.espresso.Espresso.pressBack()
            Thread.sleep(1000)
            onView(withId(R.id.analysisLegalReferencesUnlock)).perform(click())
            onView(withId(R.id.unlockConfirmationConfirm)).check(matches(isDisplayed()))
        }
    }
}
