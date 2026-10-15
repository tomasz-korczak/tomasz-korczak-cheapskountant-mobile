package pl.cheapskountant.mobile.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class MonthMathTest {
    @Test
    fun emptyPayloadIsZeroPln() {
        val figures = MonthMath.summarize(emptyList())
        assertEquals("PLN", figures.currency)
        assertEquals(0, BigDecimal("0.00").compareTo(figures.total))
        assertTrue(figures.categories.isEmpty())
    }

    @Test
    fun sumsAtScaleTwoAndKeepsPayloadCurrency() {
        val figures = MonthMath.summarize(
            listOf(
                MonthInput("Jedzenie", "EUR", BigDecimal("10.10")),
                MonthInput("Jedzenie", "EUR", BigDecimal("0.20")),
                MonthInput("Transport", "EUR", BigDecimal("1.005")),
            ),
        )
        assertEquals("EUR", figures.currency)
        assertEquals(0, BigDecimal("11.31").compareTo(figures.total))
        assertEquals("Jedzenie", figures.categories[0].category)
        assertEquals(0, BigDecimal("10.30").compareTo(figures.categories[0].amount))
    }

    @Test
    fun positiveShareRoundsHalfUpAndKeepsZeroPercentRow() {
        val figures = MonthMath.summarize(
            listOf(
                MonthInput("Jedzenie", "PLN", BigDecimal("1.00")),
                MonthInput("Inne", "PLN", BigDecimal("0.004")),
            ),
        )
        val small = figures.categories.single { it.category == "Inne" }
        assertEquals(0, small.percent)
        assertTrue(small.ratio != null && small.ratio.signum() > 0)
        assertEquals(false, small.emptyBar)
    }

    @Test
    fun negativeAmountHasSignedPercentAndEmptyBar() {
        val figures = MonthMath.summarize(
            listOf(
                MonthInput("Jedzenie", "PLN", BigDecimal("80.00")),
                MonthInput("Zwrot", "PLN", BigDecimal("-20.00")),
            ),
        )
        val refund = figures.categories.single { it.category == "Zwrot" }
        assertEquals(0, BigDecimal("-20.00").compareTo(refund.amount))
        assertEquals(-33, refund.percent)
        assertNull(refund.ratio)
        assertTrue(refund.emptyBar)
    }

    @Test
    fun zeroAmountIsOmitted() {
        val figures = MonthMath.summarize(
            listOf(
                MonthInput("Jedzenie", "PLN", BigDecimal("5.00")),
                MonthInput("Puste", "PLN", BigDecimal("1.00")),
                MonthInput("Puste", "PLN", BigDecimal("-1.00")),
            ),
        )
        assertTrue(figures.categories.none { it.category == "Puste" })
    }

    @Test
    fun zeroTotalOmitsPercentAndDoesNotDivide() {
        val figures = MonthMath.summarize(
            listOf(
                MonthInput("Jedzenie", "PLN", BigDecimal("5.00")),
                MonthInput("Zwrot", "PLN", BigDecimal("-5.00")),
            ),
        )
        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(figures.total))
        figures.categories.forEach { row ->
            assertNull(row.percent)
            assertNull(row.ratio)
            assertTrue(row.emptyBar)
        }
    }
}
