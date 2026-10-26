package com.example

import com.example.util.ZarMoneyFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZarMoneyFormatterTest {

    @Test
    fun formatsRandWithSouthAfricanDecimalComma() {
        val formatted = ZarMoneyFormatter.format(1250.50)
        val compact = formatted
            .replace(" ", "")
            .replace("\u00A0", "")
            .replace("\u202F", "")

        assertTrue("Expected Rand currency marker: $formatted", compact.startsWith("R"))
        assertTrue("Expected decimal comma and two cents: $formatted", compact.endsWith("1250,50"))
    }

    @Test
    fun formatsZeroWithTwoFractionDigits() {
        val formatted = ZarMoneyFormatter.format(0.0)
        val compact = formatted
            .replace(" ", "")
            .replace("\u00A0", "")
            .replace("\u202F", "")

        assertEquals("R0,00", compact)
    }

    @Test
    fun formatsNegativeAmountsWithoutLosingSign() {
        val formatted = ZarMoneyFormatter.format(-25.75)
        assertTrue("Expected a negative sign: $formatted", formatted.contains("-"))
        assertTrue("Expected decimal comma: $formatted", formatted.contains(",75"))
    }
}
