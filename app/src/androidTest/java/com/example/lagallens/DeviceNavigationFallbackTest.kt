package com.example.lagallens

import android.view.View
import android.content.Intent
import android.widget.EditText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lagallens.presentation.feature.main.ui.MainActivity
import com.example.lagallens.presentation.feature.ocr.saved.ui.OcrSavedActivity
import com.example.lagallens.presentation.feature.compare.result.ui.CompareResultActivity
import com.example.lagallens.presentation.feature.chatbot.ui.ChatbotActivity
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.startsWith
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises click handlers when the device refuses injection; does not verify touch delivery. */
@RunWith(AndroidJUnit4::class)
class DeviceNavigationFallbackTest {
    private fun performViewClick() = object : ViewAction {
        override fun getConstraints(): Matcher<View> = isDisplayed()
        override fun getDescription() = "invoke an enabled view's click handler"
        override fun perform(uiController: UiController, view: View) {
            assertTrue("View must be enabled and handle clicks", view.isEnabled && view.performClick())
        }
    }

    @Test fun dashboardContractsAndHome() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.tvViewAllContracts)).perform(performViewClick())
            onView(withId(R.id.contractAppBar)).check(matches(isDisplayed()))
            onView(withId(R.id.navHome)).perform(performViewClick())
            onView(withId(R.id.dashboardGreeting)).check(matches(isDisplayed()))
        }
    }

    @Test fun ocrSavedReturnGoesHome() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = OcrSavedActivity.createIntent(context, "sample.pdf", "", "")
        ActivityScenario.launch<OcrSavedActivity>(intent).use {
            onView(withId(R.id.ocr_saved_return)).perform(performViewClick())
            onView(withId(R.id.dashboardGreeting)).check(matches(isDisplayed()))
        }
    }

    @Test fun mainTabsOpenAndReturnHome() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.navNotifications)).perform(performViewClick())
            onView(withId(R.id.notificationAppBar)).check(matches(isDisplayed()))
            onView(withId(R.id.navProfile)).perform(performViewClick())
            onView(withId(R.id.tvProfileName)).check(matches(isDisplayed()))
            onView(withId(R.id.navHome)).perform(performViewClick())
            onView(withId(R.id.dashboardGreeting)).check(matches(isDisplayed()))
        }
    }

    @Test fun compareTabsSwitchForKnownSamplePair() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, CompareResultActivity::class.java)
            .putExtra("compare_first_id", "office-lease")
            .putExtra("compare_second_id", "collaborator")
            .putExtra("compare_first_name", "A")
            .putExtra("compare_second_name", "B")
        ActivityScenario.launch<CompareResultActivity>(intent).use {
            Thread.sleep(900)
            onView(withId(R.id.compareTabOverview)).perform(performViewClick())
            onView(withId(R.id.compareOverviewContent)).check(matches(isDisplayed()))
            onView(withId(R.id.compareTabRisk)).perform(performViewClick())
            onView(withId(R.id.compareRiskContent)).check(matches(isDisplayed()))
            onView(withId(R.id.compareTabClauses)).perform(performViewClick())
            onView(withId(R.id.compareClausesContent)).check(matches(isDisplayed()))
        }
    }

    @Test fun chatbotSendsQuestionAndShowsMockReply() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = ChatbotActivity.createIntent(context, "Hợp đồng lao động mẫu", "Hợp đồng lao động")
        ActivityScenario.launch<ChatbotActivity>(intent).use { scenario ->
            scenario.onActivity { it.findViewById<EditText>(R.id.chatbotInput).setText("Kiểm tra hợp đồng") }
            onView(withId(R.id.chatbotSend)).perform(performViewClick())
            onView(withText("Kiểm tra hợp đồng")).check(matches(isDisplayed()))
            onView(allOf(
                withId(R.id.chatbotAssistantMessage),
                withText(startsWith("[DEMO] Tôi đã ghi nhận câu hỏi"))
            )).check(matches(isDisplayed()))
        }
    }
}
