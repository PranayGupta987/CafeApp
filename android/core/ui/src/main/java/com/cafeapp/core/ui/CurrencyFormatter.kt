package com.cafeapp.core.ui

import java.text.NumberFormat
import java.util.Locale

fun formatPricePaise(paise: Int): String {
    val rupees = paise / 100.0
    return if (paise % 100 == 0) {
        "₹${paise / 100}"
    } else {
        String.format(Locale.getDefault(), "₹%.2f", rupees)
    }
}

object CurrencyFormatter {
    fun formatPricePaise(paise: Int): String = com.cafeapp.core.ui.formatPricePaise(paise)
}
