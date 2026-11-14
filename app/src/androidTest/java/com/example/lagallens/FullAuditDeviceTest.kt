package com.example.lagallens

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasErrorText
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lagallens.presentation.feature.compare.select.ui.CompareSelectActivity
import com.example.lagallens.presentation.feature.login.ui.LoginActivity
import com.example.lagallens.presentation.feature.ocr.editor.ui.OcrEditorActivity
import com.example.lagallens.presentation.feature.chatbot.ui.ChatbotActivity
import com.example.lagallens.presentation.feature.analysis.risk.findings.ui.AnalysisRiskFindingsActivity
import com.example.lagallens.presentation.feature.analysis.legalreferences.ui.AnalysisLegalReferencesActivity
import com.example.lagallens.presentation.feature.main.ui.MainActivity
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import org.hamcrest.Matchers.not
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FullAuditDeviceTest {
    @get:org.junit.Rule val demoSession = com.example.lagallens.DemoSessionRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun emptyLoginShowsRequiredErrors() {
        ActivityScenario.launch(LoginActivity::class.java).use {
            onView(withId(R.id.emailInput)).perform(replaceText(""))
            onView(withId(R.id.passwordInput)).perform(replaceText(""), closeSoftKeyboard())
            onView(withId(R.id.submitButton)).perform(click())
            onView(withId(R.id.emailInput)).check(matches(hasErrorText(context.getString(R.string.login_username_required))))
            onView(withId(R.id.passwordInput)).check(matches(hasErrorText(context.getString(R.string.login_password_required))))
        }
    }

    @Test fun compareRequiresTwoDistinctSampleContracts() {
        ActivityScenario.launch<CompareSelectActivity>(CompareSelectActivity.createIntent(context)).use {
            onView(withId(R.id.compareSelectSubmit)).check(matches(not(isEnabled())))
            onView(seededContractTitle(R.string.contract_demo_labor_indefinite)).perform(click())
            onView(withId(R.id.compareSelectSubmit)).check(matches(not(isEnabled())))
            onView(seededContractTitle(R.string.contract_demo_labor_fixed_term)).perform(click())
            onView(withId(R.id.compareSelectSubmit)).check(matches(isEnabled()))
            onView(withId(R.id.compareSelectSubmit)).perform(click())
            onView(withId(R.id.compareResultRoot)).check(matches(isDisplayed()))
            pressBack()
            onView(withId(R.id.compareSelectSubmit)).check(matches(isDisplayed()))
        }
    }

    @Test fun ocrEditorTypingAndRestoreUseFixtureOnly() {
        val original = "Fixture text"
        val intent = OcrEditorActivity.createIntent(context, original)
        ActivityScenario.launch<OcrEditorActivity>(intent).use {
            onView(withId(R.id.ocr_editor_text)).perform(click(), typeText(" added"), closeSoftKeyboard())
            onView(withId(R.id.ocr_editor_restore)).perform(click())
            onView(withId(R.id.ocr_editor_text)).check(matches(withText(original)))
        }
    }

    @Test fun chatbotPickerOpensAndAndroidBackCancelsIt() {
        val intent = ChatbotActivity.createIntent(context, "Fixture", "Fixture")
        ActivityScenario.launch<ChatbotActivity>(intent).use {
            val pickerSeen = AtomicBoolean(false)
            val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
            val cancel = thread {
                repeat(80) {
                    val packageName = automation.rootInActiveWindow?.packageName?.toString().orEmpty()
                    if (packageName.contains("documentsui", ignoreCase = true)) {
                        pickerSeen.set(true)
                        automation.executeShellCommand("input keyevent 4").close()
                        return@thread
                    }
                    Thread.sleep(100)
                }
            }
            onView(withId(R.id.chatbotAttach)).perform(click())
            onView(withId(R.id.chatbotAttachmentPdf)).perform(click())
            cancel.join(9000)
            assertTrue("System document picker did not appear", pickerSeen.get())
            onView(withId(R.id.chatbotInput)).check(matches(isDisplayed()))
        }
    }

    @Test fun eachRiskFilterOpensMatchingSampleDetail() {
        val intent = com.example.lagallens.AnalysisTestFixture.scoped(AnalysisRiskFindingsActivity.createIntent(context, "fixture.pdf", "PDF"))
        val cases = listOf(
            R.id.analysisRiskFindingsFilterHigh to R.string.analysis_risk_findings_penalty_title,
            R.id.analysisRiskFindingsFilterMedium to R.string.analysis_risk_findings_termination_title,
            R.id.analysisRiskFindingsFilterLow to R.string.analysis_risk_findings_dispute_title
        )
        ActivityScenario.launch<AnalysisRiskFindingsActivity>(intent).use { scenario ->
            cases.forEach { (filter, title) ->
                var expectedTitle = ""
                scenario.onActivity { expectedTitle = it.getString(R.string.analysis_snapshot_demo_metadata, it.getString(title)) }
                onView(withId(filter)).perform(scrollTo(), click())
                onView(withText(title)).perform(click())
                onView(withId(R.id.analysisRiskDetailTitle)).check(matches(withText(expectedTitle)))
                pressBack()
            }
        }
    }

    /** DEMO shows sample references, labelled as samples rather than verified citations. */
    @Test fun legalReferencesShowLabelledDemoSamples() {
        val intent = com.example.lagallens.AnalysisTestFixture.scoped(AnalysisLegalReferencesActivity.createIntent(context, "fixture.pdf", "PDF"))
        ActivityScenario.launch<AnalysisLegalReferencesActivity>(intent).use {
            onView(withId(R.id.analysisLegalReferencesSectionTitle)).check(matches(withText(R.string.analysis_legal_demo_heading)))
            onView(withId(R.id.analysisLegalReferencesDemo)).check(matches(isDisplayed()))
        }
    }

    /** The phone number is optional; only the empty name blocks saving. */
    @Test fun profileEmptyNameIsRejectedButEmptyPhoneIsAllowed() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.navProfile)).perform(click())
            onView(withId(R.id.actionEditProfile)).perform(scrollTo(), click())
            onView(withId(R.id.etProfileName)).perform(replaceText(""))
            onView(withId(R.id.etProfilePhone)).perform(replaceText(""), closeSoftKeyboard())
            onView(withId(R.id.btnSaveProfile)).perform(click())
            onView(withId(R.id.tvProfileNameError)).check(matches(isDisplayed()))
            onView(withId(R.id.tvProfilePhoneError)).check(matches(not(isDisplayed())))
        }
    }
}
