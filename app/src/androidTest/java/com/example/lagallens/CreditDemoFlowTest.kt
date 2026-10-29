package com.example.lagallens

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lagallens.presentation.feature.creditstore.flow.ui.PurchaseSuccessActivity
import com.example.lagallens.data.repository.FakeCreditRepository
import kotlinx.coroutines.runBlocking
import java.util.UUID
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CreditDemoFlowTest {
    @Test fun completedPurchaseUpdatesWalletAndStore() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences("credit_demo_wallet", android.content.Context.MODE_PRIVATE).edit().clear().commit()
        FakeCreditRepository(context)
        val app = context.applicationContext as LegalLensApplication
        val paymentId = UUID.randomUUID().toString()
        runBlocking {
            app.paymentRepository.createOrGet(paymentId, "credits_5", 29000, 5)
            app.paymentRepository.completeDemoPayment(paymentId)
        }
        val intent = Intent(context, PurchaseSuccessActivity::class.java)
            .putExtra("credit_purchase_package_id", "credits_5")
            .putExtra("credit_purchase_balance", 5)
            .putExtra("credit_purchase_id", paymentId)
        ActivityScenario.launch<PurchaseSuccessActivity>(intent).use {
            onView(withId(R.id.successBalanceAfter)).check(matches(withText("10 Credits")))
            onView(withId(R.id.purchaseSuccessReturnToUnlock)).perform(click())
            onView(withId(R.id.creditHubBalance)).check(matches(withText("10 Credits")))
            onView(withId(R.id.creditHubBuy)).perform(click())
            onView(withId(R.id.creditStoreBalance)).check(matches(withText("10 Credits")))
        }
    }
}
