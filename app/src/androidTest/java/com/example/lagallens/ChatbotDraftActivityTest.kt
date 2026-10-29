package com.example.lagallens

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.startsWith
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lagallens.presentation.feature.chatbot.ui.ChatbotActivity
import com.example.lagallens.presentation.feature.chatbot.history.ui.ChatbotHistoryActivity
import android.os.SystemClock
import org.junit.Test

class ChatbotDraftActivityTest {
    @Test
    fun newConversationCanReviewLocalDraftRequest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = ChatbotActivity.createNewConversationIntent(
            context,
            context.getString(R.string.chatbot_default_contract_name),
            context.getString(R.string.chatbot_default_contract_type)
        )

        ActivityScenario.launch<ChatbotActivity>(intent).use {
            onView(withId(R.id.chatbotEmptyState)).check(matches(isDisplayed()))
            onView(withId(R.id.chatbotNewSuggestionRisk)).perform(click())
            onView(withText(R.string.chatbot_draft_clarification)).check(matches(isDisplayed()))
            onView(withId(R.id.chatbotAttach)).perform(click())
            onView(withId(R.id.chatbotAttachmentCancel)).check(matches(isDisplayed()))
            pressBack()
            onView(withId(R.id.chatbotInput))
                .perform(replaceText(context.getString(R.string.chatbot_draft_sample_details)))
            onView(withId(R.id.chatbotInput)).perform(closeSoftKeyboard())
            onView(withId(R.id.chatbotSend)).perform(click())
            onView(withId(R.id.chatbotDraftReviewAction)).check(matches(isDisplayed()))
            onView(withId(R.id.chatbotDraftReviewAction)).perform(click())
            onView(withId(R.id.chatbotDraftReview)).check(matches(isDisplayed()))
            onView(withId(R.id.chatbotDraftCreate)).check(matches(isDisplayed()))
            onView(withId(R.id.chatbotDraftCreate)).perform(click())
            onView(withId(R.id.chatbotGenerationLoading)).check(matches(isDisplayed()))
            SystemClock.sleep(2200)
            onView(withId(R.id.chatbotGenerationSuccess)).check(matches(isDisplayed()))
            onView(withId(R.id.chatbotGenerationView)).perform(scrollTo(), click())
            onView(withId(R.id.draftPreviewRoot)).check(matches(isDisplayed()))
            onView(withId(R.id.draftContractName)).check(matches(withText(context.getString(R.string.chatbot_draft_document_name, "D1"))))
        }
        ActivityScenario.launch(ChatbotHistoryActivity::class.java).use {
            onView(withId(R.id.chatbotHistoryList)).check(matches(isDisplayed()))
        }
        ActivityScenario.launch<ChatbotActivity>(ChatbotActivity.createIntent(
            context,
            context.getString(R.string.chatbot_default_contract_name),
            context.getString(R.string.chatbot_default_contract_type)
        )).use {
            onView(withId(R.id.chatbotSuggestionContractTerm)).perform(click())
            onView(withId(R.id.chatbotNewDraftFromDocument)).check(matches(isDisplayed()))
            onView(allOf(
                withId(R.id.chatbotAssistantMessage),
                withText(startsWith("[DEMO] Tôi đã ghi nhận câu hỏi"))
            )).check(matches(isDisplayed()))
            onView(withId(R.id.chatbotNewDraftFromDocument)).perform(click())
            onView(withId(R.id.chatbotDraftLoading)).check(matches(isDisplayed()))
            SystemClock.sleep(1800)
            onView(withText(R.string.chatbot_draft_clarification)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun failedDraftRetainsRequestAndCanRetry() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = ChatbotActivity.createNewConversationIntent(
            context, context.getString(R.string.chatbot_default_contract_name),
            context.getString(R.string.chatbot_default_contract_type), previewDraftError = true
        )
        ActivityScenario.launch<ChatbotActivity>(intent).use {
            onView(withId(R.id.chatbotNewSuggestionRisk)).perform(click())
            onView(withText(R.string.chatbot_draft_clarification)).check(matches(isDisplayed()))
            onView(withId(R.id.chatbotInput)).perform(replaceText(context.getString(R.string.chatbot_draft_sample_details)), closeSoftKeyboard())
            onView(withId(R.id.chatbotSend)).perform(click())
            onView(withId(R.id.chatbotDraftReviewAction)).perform(click())
            onView(withId(R.id.chatbotDraftCreate)).perform(scrollTo(), click())
            SystemClock.sleep(2200)
            onView(withId(R.id.chatbotGenerationError)).check(matches(isDisplayed()))
            onView(withId(R.id.chatbotGenerationRequest)).check(matches(withText(R.string.chatbot_generation_request)))
            onView(withId(R.id.chatbotGenerationRetry)).perform(scrollTo(), click())
            SystemClock.sleep(2200)
            onView(withId(R.id.chatbotGenerationSuccess)).check(matches(isDisplayed()))
        }
    }

}
