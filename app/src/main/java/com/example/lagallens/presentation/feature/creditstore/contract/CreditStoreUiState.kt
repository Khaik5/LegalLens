package com.example.lagallens.presentation.feature.creditstore.contract

import androidx.annotation.StringRes
import com.example.lagallens.R

data class CreditStoreUiState(
    val balanceCredits: Int = 5,
    val packages: List<CreditPackage> = CreditPackage.samples
)

data class CreditPackage(
    val id: String,
    val credits: Int,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val isSelected: Boolean = false
) {
    companion object {
        val samples = listOf(
            CreditPackage(
                id = "credits_5",
                credits = 5,
                titleRes = R.string.credit_store_package_5_title,
                descriptionRes = R.string.credit_store_package_5_description
            ),
            CreditPackage(
                id = "credits_15",
                credits = 15,
                titleRes = R.string.credit_store_package_15_title,
                descriptionRes = R.string.credit_store_package_15_description,
                isSelected = true
            ),
            CreditPackage(
                id = "credits_30",
                credits = 30,
                titleRes = R.string.credit_store_package_30_title,
                descriptionRes = R.string.credit_store_package_30_description
            ),
            CreditPackage(
                id = "credits_100",
                credits = 100,
                titleRes = R.string.credit_store_package_100_title,
                descriptionRes = R.string.credit_store_package_100_description
            )
        )
    }
}
