package com.example.util

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Shared South African Rand formatting for user-facing financial amounts.
 *
 * This helper formats existing major-unit Double values consistently. It does not
 * change database storage; converting persisted Double values to minor units requires
 * a separate, explicit data migration.
 */
object ZarMoneyFormatter {
    private val southAfricanLocale = Locale.forLanguageTag("en-ZA")

    fun format(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(southAfricanLocale).apply {
            currency = Currency.getInstance("ZAR")
            minimumFractionDigits = 2
            maximumFractionDigits = 2
            isGroupingUsed = true
        }
        return formatter.format(amount)
    }
}
