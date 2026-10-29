package com.example.lagallens

import android.content.Intent
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import org.junit.Assert.assertEquals
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lagallens.presentation.feature.main.ui.MainActivity
import com.example.lagallens.presentation.feature.chatbot.ui.ChatbotActivity
import com.example.lagallens.presentation.feature.chatbot.history.ui.ChatbotHistoryActivity
import com.example.lagallens.presentation.feature.compare.result.ui.CompareResultActivity
import com.example.lagallens.presentation.feature.analysis.result.ui.AnalysisResultActivity
import com.example.lagallens.presentation.feature.creditstore.ui.CreditStoreActivity
import org.junit.Test
import org.junit.runner.RunWith
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.startsWith

@RunWith(AndroidJUnit4::class)
class DeviceInteractionTest {
    @Test fun mainTabsOpenAndReturnHome() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.navNotifications)).perform(click())
            onView(withId(R.id.notificationAppBar)).check(matches(isDisplayed()))
            onView(withId(R.id.navProfile)).perform(click())
            onView(withId(R.id.tvProfileName)).check(matches(isDisplayed()))
            onView(withId(R.id.navHome)).perform(click())
            onView(withId(R.id.dashboardGreeting)).check(matches(isDisplayed()))
        }
    }

    @Test fun compareAllThreeTabsForSamplePair() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, CompareResultActivity::class.java)
            .putExtra("compare_first_id", "office-lease")
            .putExtra("compare_second_id", "collaborator")
            .putExtra("compare_first_name", "A")
            .putExtra("compare_second_name", "B")
        ActivityScenario.launch<CompareResultActivity>(intent).use {
            Thread.sleep(900)
            onView(withId(R.id.compareTabOverview)).perform(click())
            onView(withId(R.id.compareOverviewContent)).check(matches(isDisplayed()))
            onView(withId(R.id.compareTabRisk)).perform(click())
            onView(withId(R.id.compareRiskContent)).check(matches(isDisplayed()))
            onView(withId(R.id.compareTabClauses)).perform(click())
            onView(withId(R.id.compareClausesContent)).check(matches(isDisplayed()))
        }
    }

    @Test fun chatbotSendsQuestionAndShowsMockReply() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = ChatbotActivity.createIntent(context, "Hợp đồng lao động mẫu", "Hợp đồng lao động")
        ActivityScenario.launch<ChatbotActivity>(intent).use { scenario ->
            scenario.onActivity { it.findViewById<EditText>(R.id.chatbotInput).setText("Kiểm tra hợp đồng") }
            onView(withId(R.id.chatbotSend)).perform(click())
            onView(withText("Kiểm tra hợp đồng")).check(matches(isDisplayed()))
            onView(allOf(
                withId(R.id.chatbotAssistantMessage),
                withText(startsWith("[DEMO] Tôi đã ghi nhận câu hỏi"))
            )).check(matches(isDisplayed()))
        }
    }

    @Test fun contractDetailViewerAndSafeDialogs() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.navContracts)).perform(click())
            onView(withText(R.string.contract_demo_labor_indefinite)).perform(click())
            onView(withId(R.id.tvContractTitle)).check(matches(withText(R.string.contract_demo_labor_indefinite)))
            onView(withId(R.id.btnRename)).perform(click())
            onView(withId(R.id.renameCancel)).perform(click())
            onView(withId(R.id.btnDelete)).perform(click())
            onView(withId(R.id.btnCancel)).perform(click())
            onView(withText(R.string.contract_detail_view_document)).perform(click())
            onView(withId(R.id.viewerRoot)).check(matches(isDisplayed()))
            pressBack()
            onView(withId(R.id.tvContractTitle)).check(matches(withText(R.string.contract_demo_labor_indefinite)))
            pressBack()
            onView(withText(R.string.contract_demo_labor_fixed_term)).perform(click())
            onView(withId(R.id.tvContractTitle)).check(matches(withText(R.string.contract_demo_labor_fixed_term)))
        }
    }

    @Test fun creditStoreSelectionPassesAmountAndPrice() {
        ActivityScenario.launch(CreditStoreActivity::class.java).use {
            onView(allOf(withId(R.id.creditStorePackageTitle), withText(R.string.credit_store_package_5_title))).perform(scrollTo(), click())
            onView(withId(R.id.creditStoreTopUpAction)).perform(scrollTo(), click())
            onView(withId(R.id.purchasePackageTitle)).check(matches(withText("5 Credits")))
            onView(withId(R.id.purchaseSummaryPrice)).check(matches(withText("29.000đ")))
        }
    }

    @Test fun profileAppearanceOpensSettings() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.navProfile)).perform(click())
            onView(withId(R.id.actionAppearance)).perform(scrollTo(), click())
            onView(withId(R.id.settingsAppBar)).check(matches(isDisplayed()))
            pressBack()
            onView(withId(R.id.tvProfileName)).check(matches(isDisplayed()))
        }
    }

    @Test fun chatbotHistorySearchShowsEmptyForUnknownQuery() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = ChatbotHistoryActivity.createIntent(context)
        ActivityScenario.launch<ChatbotHistoryActivity>(intent).use {
            onView(withId(R.id.chatbotHistorySearch)).perform(click())
            Thread.sleep(600)
            onView(withId(R.id.chatbotHistoryQuery)).perform(replaceText("zz_no_match_987"), closeSoftKeyboard())
            onView(withId(R.id.chatbotHistoryEmpty)).check(matches(isDisplayed()))
        }
    }

    @Test fun chatbotDrawerHistorySearchOpens() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = ChatbotActivity.createIntent(context, "Hợp đồng lao động mẫu", "Hợp đồng lao động")
        ActivityScenario.launch<ChatbotActivity>(intent).use {
            onView(withId(R.id.chatbotBack)).perform(click())
            onView(withId(R.id.chatbotDrawerHistory)).perform(click())
            Thread.sleep(800)
            onView(withId(R.id.chatbotHistorySearch)).perform(click())
            Thread.sleep(600)
            onView(withId(R.id.chatbotHistoryQuery)).check(matches(isDisplayed()))
        }
    }

    @Test fun notificationFeedShowsPersistedDemoRecords() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.navNotifications)).perform(click())
            Thread.sleep(700)
            onView(withText(R.string.notification_analysis_complete_title)).check(matches(isDisplayed()))
        }
    }

    @Test fun analysisFromContractReturnsToCorrectDetail() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.navContracts)).perform(click())
            onView(withText(R.string.contract_demo_labor_indefinite)).perform(click())
            onView(withText(R.string.contract_detail_analyze)).perform(click())
            onView(withId(R.id.analysis_start_action)).perform(click())
            Thread.sleep(6000)
            onView(withId(R.id.analysis_result_document_name)).check(matches(withText(R.string.contract_demo_labor_indefinite)))
            pressBack()
            onView(withId(R.id.tvContractTitle)).check(matches(withText(R.string.contract_demo_labor_indefinite)))
        }
    }

    @Test fun analysisResultSectionsOpenAndReturn() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = AnalysisResultActivity.createIntent(context, "sample.pdf", "PDF")
        val sections = listOf(
            R.string.analysis_result_summary_title to R.id.analysis_summary_root,
            R.string.analysis_result_important_information_title to R.id.analysis_key_facts_root,
            R.string.analysis_result_parties_title to R.id.analysis_parties_root,
            R.string.analysis_result_rights_title to R.id.analysis_obligations_root,
            R.string.analysis_result_risk_title to R.id.analysisRiskOverviewRoot,
            R.string.analysis_result_legal_basis_title to R.id.analysisLegalReferencesRoot
        )
        ActivityScenario.launch<AnalysisResultActivity>(intent).use {
            sections.forEach { (title, root) ->
                if (title == R.string.analysis_result_legal_basis_title) {
                    onView(withId(R.id.analysis_result_root)).perform(swipeUp(), swipeUp(), swipeUp())
                    Thread.sleep(300)
                }
                onView(allOf(withId(R.id.analysis_result_item_title), withText(title))).perform(scrollTo(), click())
                onView(withId(root)).check(matches(isDisplayed()))
                pressBack()
                onView(withId(R.id.analysis_result_document_name)).check(matches(withText("sample.pdf")))
            }
        }
    }

    @Test fun analysisResultContainsAllSixSections() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = AnalysisResultActivity.createIntent(context, "sample.pdf", "PDF")
        ActivityScenario.launch<AnalysisResultActivity>(intent).use { scenario ->
            Thread.sleep(500)
            scenario.onActivity { activity ->
                val list = activity.findViewById<RecyclerView>(R.id.analysis_result_items)
                assertEquals("Analysis result section count", 6, list.adapter?.itemCount)
            }
        }
    }
}
