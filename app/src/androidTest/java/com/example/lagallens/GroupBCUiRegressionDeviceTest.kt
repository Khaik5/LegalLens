package com.example.lagallens

import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lagallens.domain.model.credit.*
import com.example.lagallens.domain.model.payment.*
import com.example.lagallens.domain.repository.*
import com.example.lagallens.presentation.feature.creditstore.flow.ui.CreditHistoryActivity
import com.example.lagallens.presentation.feature.creditstore.flow.ui.PaymentPendingActivity
import com.example.lagallens.presentation.feature.creditstore.ui.CreditStoreActivity
import com.example.lagallens.presentation.feature.creditstore.hub.ui.CreditHubActivity
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class GroupBCUiRegressionDeviceTest {
    private val app = ApplicationProvider.getApplicationContext<LegalLensApplication>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val wallet = Wallet()
    private val payments = Payments()

    @Before fun configureSandboxOnly() { app.setSandboxFinance(wallet, payments) }
    @After fun restoreRepositories() { app.setSandboxFinance(null, null) }

    @Test fun paymentCountdownStopsInBackgroundAndDoesNotMultiplyOnResume() {
        val intent = Intent(app, PaymentPendingActivity::class.java)
            .putExtra("credit_purchase_package_id", "test")
            .putExtra("credit_purchase_credits", 5)
            .putExtra("credit_purchase_balance", 5)
            .putExtra("credit_purchase_id", "pending-fixture")
        ActivityScenario.launch<PaymentPendingActivity>(intent).use { scenario ->
            scenario.onActivity { it.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
            val updates = AtomicInteger()
            lateinit var countdown: TextView
            await { scenario.onActivity { countdown = it.findViewById(R.id.pendingQrCountdown) }; countdown.text.isNotEmpty() }
            scenario.onActivity {
                countdown.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { updates.incrementAndGet() }
                    override fun afterTextChanged(s: Editable?) = Unit
                })
            }
            repeat(2) {
                scenario.moveToState(Lifecycle.State.CREATED)
                instrumentation.waitForIdleSync()
                val stopped = updates.get()
                android.os.SystemClock.sleep(1_250)
                assertEquals("Stopped Activity must not receive timer updates", stopped, updates.get())
                scenario.moveToState(Lifecycle.State.RESUMED)
                instrumentation.waitForIdleSync()
            }
            val resumed = updates.get()
            android.os.SystemClock.sleep(1_250)
            assertTrue("Only one countdown may update the view", updates.get() - resumed in 1..2)
        }
    }

    @Test fun creditHistoryUsesItsOwnContainerAfterTheLayoutOrderChanges() {
        ActivityScenario.launch<CreditHistoryActivity>(CreditHistoryActivity.createIntent(app)).use { scenario ->
            scenario.onActivity { it.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
            lateinit var rows: LinearLayout
            await {
                var ready = false
                scenario.onActivity { activity ->
                    val id = activity.resources.getIdentifier("creditHistoryTransactions", "id", app.packageName)
                    assertTrue("History needs a named transaction container", id != 0)
                    rows = activity.findViewById(id)
                    ready = rows.childCount == 2
                }
                ready
            }
            scenario.onActivity { activity ->
                (rows.parent as ViewGroup).addView(View(activity), 0)
                activity.findViewById<View>(R.id.tabCreditHistoryUsage).performClick()
                assertEquals(1, rows.childCount)
                assertTrue((rows.getChildAt(0) as TextView).text.contains("−2"))
            }
        }
    }

    @Test fun storeShowsTransientErrorAndRetryReloadsPackagesAndBalance() {
        wallet.fail = true
        payments.fail = true
        ActivityScenario.launch<CreditStoreActivity>(CreditStoreActivity.createIntent(app)).use { scenario ->
            scenario.onActivity { it.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
            await {
                var ready = false
                scenario.onActivity { activity ->
                    val id = activity.resources.getIdentifier("creditStoreRetry", "id", app.packageName)
                    assertTrue("Store needs a retry control", id != 0)
                    ready = activity.findViewById<View>(id).visibility == View.VISIBLE
                }
                ready
            }
            wallet.fail = false
            payments.fail = false
            scenario.onActivity { activity ->
                activity.findViewById<View>(activity.resources.getIdentifier("creditStoreRetry", "id", app.packageName)).performClick()
            }
            await {
                var ready = false
                scenario.onActivity { activity ->
                    ready = activity.findViewById<View>(activity.resources.getIdentifier("creditStoreRetry", "id", app.packageName)).visibility == View.GONE
                }
                ready
            }
            assertEquals(2, payments.loads.get())
            assertEquals(2, wallet.loads.get())
        }
    }

    @Test fun hubRetryReloadsTheBalanceAfterATransientFailure() {
        wallet.fail = true
        ActivityScenario.launch<CreditHubActivity>(CreditHubActivity.createIntent(app)).use { scenario ->
            scenario.onActivity { it.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
            await {
                var visible = false
                scenario.onActivity { visible = it.findViewById<View>(R.id.creditHubRetry).visibility == View.VISIBLE }
                visible
            }
            wallet.fail = false
            scenario.onActivity { it.findViewById<View>(R.id.creditHubRetry).performClick() }
            await {
                var recovered = false
                scenario.onActivity { recovered = it.findViewById<View>(R.id.creditHubRetry).visibility == View.GONE }
                recovered
            }
            assertEquals(2, wallet.loads.get())
        }
    }

    @Test fun cancelledHistoryLoadDoesNotRenderANetworkFailure() {
        wallet.cancelRefresh = true
        ActivityScenario.launch<CreditHistoryActivity>(CreditHistoryActivity.createIntent(app)).use { scenario ->
            scenario.onActivity { it.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
            await { wallet.loads.get() == 1 }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val rows = activity.findViewById<LinearLayout>(R.id.creditHistoryTransactions)
                assertEquals(1, rows.childCount)
                assertEquals(activity.getString(R.string.credit_history_loading), (rows.getChildAt(0) as TextView).text.toString())
            }
        }
    }

    private fun await(condition: () -> Boolean) {
        val deadline = android.os.SystemClock.uptimeMillis() + 5_000
        while (!condition()) {
            check(android.os.SystemClock.uptimeMillis() < deadline) { "UI did not settle" }
            android.os.SystemClock.sleep(50)
        }
    }

    private class Wallet : CreditRepository {
        @Volatile var fail = false
        @Volatile var cancelRefresh = false
        val loads = AtomicInteger()
        override val balance = MutableStateFlow(5)
        override fun activeSessionKey() = "bc-fixture"
        override suspend fun refresh() {
            loads.incrementAndGet()
            if (cancelRefresh) throw kotlinx.coroutines.CancellationException("cancelled")
            if (fail) throw IOException("offline")
        }
        override suspend fun spend(transactionId: String, credits: Int): CreditReceipt = error("No spending in this test")
        override suspend fun getTransactions() = listOf(
            CreditTransaction("purchase", null, null, 0, 5, "COMPLETED", 1, CreditTransactionKind.PURCHASE),
            CreditTransaction("usage", null, null, 0, -2, "COMPLETED", 2, CreditTransactionKind.USAGE)
        )
    }

    private class Payments : PaymentRepository {
        @Volatile var fail = false
        val loads = AtomicInteger()
        override fun activeSessionKey() = "bc-fixture"
        override suspend fun getPackages(): List<CreditOffer> {
            loads.incrementAndGet()
            if (fail) throw IOException("offline")
            return listOf(CreditOffer("test", 5, 29_000, "5 Credits", "", true))
        }
        private fun record(id: String) = PaymentRecord(id, "test", 29_000, 5, PaymentStatus.PENDING, 0)
        override suspend fun createPayment(paymentId: String, packageId: String) = record(paymentId)
        override fun observePayment(paymentId: String) = flow { emit(record(paymentId)); awaitCancellation() }
        override suspend fun getBankTransfer(paymentId: String) = BankTransfer(paymentId, 29_000, "TEST", "", System.currentTimeMillis() + 60_000)
        override suspend fun cancelPayment(paymentId: String) = error("No cancellation in this test")
        override suspend fun get(paymentId: String) = record(paymentId)
    }
}
