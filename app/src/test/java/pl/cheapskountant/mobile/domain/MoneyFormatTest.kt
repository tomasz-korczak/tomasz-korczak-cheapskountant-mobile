package pl.cheapskountant.mobile.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

class MoneyFormatTest {
    @Test
    fun polishPlnUsesCommaAndZlotySuffix() {
        val formatted = MoneyFormat.format(BigDecimal("12.50"), "PLN", Locale("pl"))
        assertTrue(formatted.endsWith(" zł"))
        assertTrue(formatted.contains("12,50"))
    }

    @Test
    fun polishEuroAndDollarAreSuffixesWithGroupedThousands() {
        val euro = MoneyFormat.format(BigDecimal("1234.50"), "EUR", Locale("pl"))
        val dollar = MoneyFormat.format(BigDecimal("1234.50"), "USD", Locale("pl"))
        assertTrue(euro.endsWith(" €"))
        assertTrue(dollar.endsWith(" $"))
        assertTrue(euro.contains(",50"))
        assertTrue(euro.contains("234"))
    }

    @Test
    fun dayListAlwaysUsesZloty() {
        val formatted = MoneyFormat.format(BigDecimal("44.87"), "EUR", Locale.ENGLISH, dayList = true)
        assertTrue(formatted.endsWith(" zł"))
    }

    @Test
    fun alwaysShowsTwoFractionDigits() {
        val formatted = MoneyFormat.format(BigDecimal("3"), "PLN", Locale.ENGLISH)
        assertTrue(formatted.startsWith("3.00 "))
    }
}
