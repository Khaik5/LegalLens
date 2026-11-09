package com.example.lagallens

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lagallens.presentation.feature.creditstore.flow.ui.PurchaseSuccessActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.UUID
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PurchaseSuccessNavigationTest {
    private fun successIntent(): Intent {
        val app = ApplicationProvider.getApplicationContext<LegalLensApplication>()
        val paymentId = UUID.randomUUID().toString()
        runBlocking {
            app.paymentRepository.createPayment(paymentId, "credits_15")
            app.paymentRepository.observePayment(paymentId).first { it.status != com.example.lagallens.domain.model.payment.PaymentStatus.PENDING }
        }
        return Intent(app, PurchaseSuccessActivity::class.java)
            .putExtra("credit_purchase_package_id", "credits_15")
            .putExtra("credit_purchase_credits", 15)
            .putExtra("credit_purchase_balance", 5)
            .putExtra("credit_purchase_id", paymentId)
    }

    @Test fun actionReturnsToCreditHub() {
        ActivityScenario.launch<PurchaseSuccessActivity>(successIntent()).use {
            onView(withId(R.id.purchaseSuccessReturnToUnlock)).perform(click())
            onView(withId(R.id.creditHubRoot)).check(matches(isDisplayed()))
        }
    }

    @Test fun systemBackReturnsToCreditHub() {
        ActivityScenario.launch<PurchaseSuccessActivity>(successIntent()).use {
            pressBack()
            onView(withId(R.id.creditHubRoot)).check(matches(isDisplayed()))
        }
    }
}
