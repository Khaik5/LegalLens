package com.example.lagallens

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lagallens.domain.model.ProfileLanguage
import com.example.lagallens.presentation.feature.creditstore.flow.ui.PurchaseConfirmationActivity
import com.example.lagallens.presentation.feature.creditstore.flow.ui.PurchaseSuccessActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.UUID
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EnglishCreditAuditTest {
    private val app = ApplicationProvider.getApplicationContext<LegalLensApplication>()
    private lateinit var originalLanguage: ProfileLanguage

    @Before fun setEnglish() {
        runBlocking {
            originalLanguage = app.observeProfileSettingsUseCase().first().language
            app.updateProfileLanguageUseCase(ProfileLanguage.ENGLISH)
        }
    }

    @After fun restoreLanguage() {
        runBlocking { app.updateProfileLanguageUseCase(originalLanguage) }
    }

    private fun <T : android.app.Activity> awaitEnglish(scenario: ActivityScenario<T>) {
        val deadline = SystemClock.uptimeMillis() + 5000
        var actual = ""
        do {
            scenario.onActivity { actual = it.getString(R.string.credit_flow_purchase_title) }
            if (actual == "Confirm Credit purchase") break
            SystemClock.sleep(50)
        } while (SystemClock.uptimeMillis() < deadline)
        org.junit.Assert.assertEquals("Confirm Credit purchase", actual)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    @Test fun popularPackageDescriptionIsEnglish() {
        val intent = PurchaseConfirmationActivity.createIntent(app, "credits_15", 5)
        ActivityScenario.launch<PurchaseConfirmationActivity>(intent).use {
            awaitEnglish(it)
            onView(withText("Confirm Credit purchase")).check(matches(isDisplayed()))
            onView(withId(R.id.purchasePackageDescription)).check(matches(withText("Most popular · Never expires")))
            it.recreate()
            onView(withText("Confirm Credit purchase")).check(matches(isDisplayed()))
            onView(withId(R.id.purchasePackageDescription)).check(matches(withText("Most popular · Never expires")))
        }
    }

    @Test fun allPackagesShowSelectedAmountInEnglish() {
        val samples = listOf(Triple("credits_5", "5 Credits", "29.000đ"), Triple("credits_15", "15 Credits", "79.000đ"), Triple("credits_30", "30 Credits", "149.000đ"), Triple("credits_100", "100 Credits", "449.000đ"))
        samples.forEach { (id, title, price) ->
            ActivityScenario.launch<PurchaseConfirmationActivity>(PurchaseConfirmationActivity.createIntent(app, id, 5)).use {
                awaitEnglish(it)
                onView(withText("Confirm Credit purchase")).check(matches(isDisplayed()))
                onView(withId(R.id.purchasePackageTitle)).check(matches(withText(title)))
                onView(withId(R.id.purchaseSummaryPrice)).check(matches(withText(price)))
                onView(withId(R.id.purchaseConfirmationPay)).check(matches(withText("Pay $price")))
            }
        }
    }

    @Test fun fiveCreditSuccessDoesNotDisplayFifteen() {
        val paymentId = UUID.randomUUID().toString()
        runBlocking {
            app.paymentRepository.createOrGet(paymentId, "credits_5", 29000, 5)
            app.paymentRepository.completeDemoPayment(paymentId)
        }
        val intent = Intent(app, PurchaseSuccessActivity::class.java)
            .putExtra("credit_purchase_package_id", "credits_5")
            .putExtra("credit_purchase_balance", 5)
            .putExtra("credit_purchase_id", paymentId)
        ActivityScenario.launch<PurchaseSuccessActivity>(intent).use {
            awaitEnglish(it)
            onView(withId(R.id.successCredits)).check(matches(withText("+5 Credits")))
            onView(withText("Đã cộng 15 Credits")).check(doesNotExist())
        }
    }
}
