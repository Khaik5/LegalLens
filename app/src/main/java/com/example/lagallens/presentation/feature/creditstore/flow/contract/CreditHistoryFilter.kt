package com.example.lagallens.presentation.feature.creditstore.flow.contract

import com.example.lagallens.domain.model.credit.CreditTransaction
import com.example.lagallens.domain.model.credit.CreditTransactionKind

/** Which transactions the tabs of Credit History show. Refunds and bonuses appear only under [ALL]. */
enum class CreditHistoryFilter {
    ALL,
    PURCHASE,
    USAGE;

    fun matches(transaction: CreditTransaction): Boolean = when (this) {
        ALL -> true
        // DEMO ledger entries have no kind, so they are told apart by their amount and sign.
        PURCHASE -> transaction.kind == CreditTransactionKind.PURCHASE || (transaction.kind == null && transaction.amountVnd > 0)
        USAGE -> transaction.kind == CreditTransactionKind.USAGE || (transaction.kind == null && transaction.creditDelta < 0)
    }
}
