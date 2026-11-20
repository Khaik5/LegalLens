package com.example.lagallens.presentation.feature.creditstore.utils

import android.graphics.Typeface
import android.util.TypedValue
import android.widget.TextView
import androidx.annotation.DimenRes
import androidx.core.content.ContextCompat
import com.example.lagallens.R

/**
 * Shows a balance, or a sentence explaining why there is none. The sentence uses body text: in the large bold
 * balance style a load error filled the card and read like a number.
 */
internal fun TextView.showCreditBalance(text: CharSequence, isMessage: Boolean, @DimenRes balanceTextSize: Int) {
    this.text = text
    setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(if (isMessage) R.dimen.sp_14 else balanceTextSize))
    setTypeface(Typeface.create(typeface, if (isMessage) Typeface.NORMAL else Typeface.BOLD))
    setTextColor(ContextCompat.getColor(context, if (isMessage) R.color.legal_lens_secondary_text else R.color.legal_lens_primary))
}
