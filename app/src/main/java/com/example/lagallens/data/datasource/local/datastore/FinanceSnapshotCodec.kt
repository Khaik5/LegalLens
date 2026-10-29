package com.example.lagallens.data.datasource.local.datastore

import com.example.lagallens.data.repository.FinanceSnapshot
import com.example.lagallens.domain.model.payment.PaymentStatus
import com.google.gson.Gson
import com.google.gson.JsonParser

/** Reject corrupt/inconsistent snapshots; never substitute a new wallet for one. */
internal object FinanceSnapshotCodec {
    private val gson = Gson()
    fun encode(state: FinanceSnapshot): String = gson.toJson(state)
    fun decode(raw: String): FinanceSnapshot {
        val json = JsonParser.parseString(raw).asJsonObject
        check(listOf("schemaVersion", "balance", "payments", "spends", "transactions").all { json.has(it) && !json.get(it).isJsonNull })
        val state = checkNotNull(gson.fromJson(json, FinanceSnapshot::class.java))
        check(state.schemaVersion == 1 && state.balance >= 0)
        checkNotNull(state.payments); checkNotNull(state.spends); checkNotNull(state.transactions)
        check(state.transactions.map { it.transactionId }.distinct().size == state.transactions.size)
        check(5L + state.transactions.sumOf { it.creditDelta.toLong() } == state.balance.toLong()) { "Wallet does not match ledger" }
        state.payments.forEach { (id, payment) ->
            check(id == payment.paymentId && id.isNotBlank() && payment.credits > 0 && payment.amountVnd >= 0)
            checkNotNull(payment.status)
            val rows = state.transactions.filter { it.paymentId == id }
            if (payment.status == PaymentStatus.SUCCESS) {
                check(rows.size == 1)
                val row = rows.single()
                check(row.transactionId == "payment:$id" && row.packageId == payment.packageId && row.amountVnd == payment.amountVnd && row.creditDelta == payment.credits && row.status == "SUCCESS")
            } else check(rows.isEmpty())
        }
        state.spends.forEach { (id, amount) ->
            check(id.isNotBlank() && amount > 0)
            check(state.transactions.count { it.transactionId == "unlock:$id" && it.paymentId == null && it.creditDelta == -amount && it.status == "SUCCESS" } == 1)
        }
        state.transactions.forEach { row ->
            if (row.paymentId != null) check(state.payments[row.paymentId]?.status == PaymentStatus.SUCCESS)
            else check(row.transactionId.startsWith("unlock:") && state.spends[row.transactionId.removePrefix("unlock:")] == -row.creditDelta)
        }
        return state
    }
}
