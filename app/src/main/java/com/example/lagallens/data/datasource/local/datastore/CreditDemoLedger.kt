package com.example.lagallens.data.datasource.local.datastore

import android.content.Context
import android.content.SharedPreferences
import com.example.lagallens.domain.model.credit.CreditReceipt
import com.example.lagallens.domain.model.credit.CreditTransaction
import com.example.lagallens.domain.repository.CreditRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Persistent DEMO wallet. Existing preference keys are retained to preserve saved balances. */
open class CreditDemoLedger(initialBalance: Int = DEFAULT_BALANCE) : CreditRepository {
    private val mutableBalance = MutableStateFlow(initialBalance)
    override val balance = mutableBalance.asStateFlow()
    private val completedPurchases = mutableSetOf<String>()
    private val completedSpends = mutableSetOf<String>()
    private val transactions = mutableListOf<CreditTransaction>()
    @Volatile private var preferences: SharedPreferences? = null

    @Synchronized
    fun initialize(context: Context) {
        val store = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        preferences = store
        mutableBalance.value = store.getInt(KEY_BALANCE, DEFAULT_BALANCE)
        completedPurchases.clear()
        completedPurchases += store.getStringSet(KEY_PURCHASES, emptySet()).orEmpty()
        completedSpends.clear()
        completedSpends += store.getStringSet(KEY_SPENDS, emptySet()).orEmpty()
        transactions.clear()
        runCatching {
            JSONArray(store.getString(KEY_TRANSACTIONS, "[]")).let { rows ->
                for (index in 0 until rows.length()) {
                    val row = rows.getJSONObject(index)
                    transactions += CreditTransaction(
                        row.getString("id"), row.optString("paymentId").takeIf(String::isNotBlank),
                        row.optString("packageId").takeIf(String::isNotBlank), row.optLong("amount"),
                        row.optInt("delta"), row.optString("status"), row.optLong("createdAt")
                    )
                }
            }
        }
    }

    override suspend fun getTransactions(): List<CreditTransaction> =
        synchronized(this) { transactions.sortedByDescending { it.createdAtEpochMillis } }

    override suspend fun addPurchasedCredits(
        paymentId: String,
        packageId: String,
        amountVnd: Long,
        credits: Int
    ): CreditReceipt = synchronized(this) {
        if (paymentId.isBlank() || packageId.isBlank() || amountVnd < 0 || credits <= 0 || !completedPurchases.add(paymentId)) {
            return@synchronized CreditReceipt(mutableBalance.value, false)
        }
        mutableBalance.value += credits
        persistBalanceAndIds()
        transactions += CreditTransaction(paymentId, paymentId, packageId, amountVnd, credits, "SUCCESS", System.currentTimeMillis())
        persistTransactions()
        CreditReceipt(mutableBalance.value, true)
    }

    override suspend fun spend(transactionId: String, credits: Int): CreditReceipt = synchronized(this) {
        if (transactionId.isBlank() || credits <= 0 || transactionId in completedSpends || mutableBalance.value < credits) {
            return@synchronized CreditReceipt(mutableBalance.value, false)
        }
        completedSpends += transactionId
        mutableBalance.value -= credits
        preferences?.edit()?.putInt(KEY_BALANCE, mutableBalance.value)
            ?.putStringSet(KEY_SPENDS, completedSpends.toSet())?.commit()?.let { check(it) }
        transactions += CreditTransaction(transactionId, null, null, 0, -credits, "SUCCESS", System.currentTimeMillis())
        persistTransactions()
        CreditReceipt(mutableBalance.value, true)
    }

    private fun persistBalanceAndIds() {
        preferences?.edit()?.putInt(KEY_BALANCE, mutableBalance.value)
            ?.putStringSet(KEY_PURCHASES, completedPurchases.toSet())?.commit()?.let { check(it) }
    }

    private fun persistTransactions() {
        val store = preferences ?: return
        val json = JSONArray()
        transactions.forEach { transaction ->
            json.put(JSONObject().put("id", transaction.transactionId).put("paymentId", transaction.paymentId)
                .put("packageId", transaction.packageId).put("amount", transaction.amountVnd)
                .put("delta", transaction.creditDelta).put("status", transaction.status)
                .put("createdAt", transaction.createdAtEpochMillis))
        }
        check(store.edit().putString(KEY_TRANSACTIONS, json.toString()).commit())
    }

    private companion object {
        const val PREFERENCES_NAME = "credit_demo_wallet"
        const val KEY_BALANCE = "balance"
        const val KEY_PURCHASES = "completed_demo_purchase_ids"
        const val KEY_SPENDS = "completed_demo_spend_ids"
        const val KEY_TRANSACTIONS = "transactions_v1"
        const val DEFAULT_BALANCE = 5
    }
}

/** One process-wide DEMO ledger so independently created repositories observe the same wallet. */
object CreditDemoLedgerStore : CreditDemoLedger()
