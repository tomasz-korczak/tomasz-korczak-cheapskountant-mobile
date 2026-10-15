package pl.cheapskountant.mobile.domain

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object CalendarDates {
    fun monthRange(yearMonth: YearMonth): Pair<String, String> {
        val from = yearMonth.atDay(1)
        val to = yearMonth.atEndOfMonth()
        return from.toString() to to.toString()
    }

    fun issuedDate(issuedAt: String): String {
        val timeIndex = issuedAt.indexOf('T')
        return if (timeIndex >= 0) issuedAt.substring(0, timeIndex) else issuedAt.take(10)
    }

    fun formatMonth(yearMonth: YearMonth, locale: Locale): String {
        return yearMonth.atDay(1).format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
    }

    fun formatDay(date: LocalDate, locale: Locale): String {
        return date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale))
    }
}
