package com.example.lagallens.presentation.feature.creditstore.utils

import java.text.NumberFormat
import java.util.Locale

/** Prices are in Vietnamese dong in every app language, so they keep the Vietnamese grouping, e.g. 29.000đ. */
internal fun formatVnd(amount: Long): String =
    NumberFormat.getNumberInstance(Locale("vi", "VN")).format(amount) + "đ"
