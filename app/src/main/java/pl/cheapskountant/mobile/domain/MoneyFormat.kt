package pl.cheapskountant.mobile.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object MoneyFormat {
    fun format(
        amount: BigDecimal,
        currency: String,
        locale: Locale,
        dayList: Boolean = false,
    ): String {
        val symbols = DecimalFormatSymbols.getInstance(locale)
        val format = DecimalFormat("#,##0.00", symbols)
        format.roundingMode = RoundingMode.HALF_UP
        val number = format.format(amount)
        val symbol = when {
            dayList || currency == "PLN" -> "zł"
            currency == "EUR" -> "€"
            currency == "USD" -> "$"
            else -> currency
        }
        return "$number $symbol"
    }
}
